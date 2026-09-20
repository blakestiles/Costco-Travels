package com.smartrebook.booking.orchestration;

import com.smartrebook.booking.client.CarSupplierGateway;
import com.smartrebook.booking.client.HotelSupplierGateway;
import com.smartrebook.booking.config.CorrelationIdFilter;
import com.smartrebook.booking.domain.*;
import com.smartrebook.booking.dto.BookingEventDto;
import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.dto.ChangeSubmitRequest;
import com.smartrebook.booking.exception.BookingNotFoundException;
import com.smartrebook.booking.exception.ChangeInProgressException;
import com.smartrebook.booking.exception.InvalidBookingStateException;
import com.smartrebook.booking.exception.SupplierTimeoutException;
import com.smartrebook.booking.exception.SupplierUnavailableException;
import com.smartrebook.booking.repository.BookingEventRepository;
import com.smartrebook.booking.repository.BookingRepository;
import com.smartrebook.booking.repository.ChangeRequestRepository;
import com.smartrebook.booking.service.TripConstants;
import com.smartrebook.booking.service.TripPricingService;
import com.smartrebook.booking.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.car.CarReservationRequest;
import com.smartrebook.contracts.car.CarReservationResponse;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelReservationRequest;
import com.smartrebook.contracts.hotel.HotelReservationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-level orchestration with compensating actions inspired by the Saga pattern - not a
 * full Saga implementation. Core invariant: a reservation-change operation must not knowingly
 * leave the member without their original valid reservation because a replacement transaction
 * partially failed. The replacement is secured before the original booking's final state changes.
 *
 * Any timeout on the car-reservation call is treated as ambiguous, never as a confirmed failure -
 * the supplier may have created the reservation and simply responded late. Both the "clean
 * failure" and "ambiguous success" cases look identical to this client (a read timeout), so both
 * route through the same RECONCILIATION_REQUIRED path; the Reconcile action is what tells them
 * apart afterward. See README: Ambiguous Supplier Outcome.
 */
@Component
public class RebookOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RebookOrchestrator.class);
    private static final List<ChangeRequestStatus> TERMINAL_STATUSES =
            List.of(ChangeRequestStatus.COMPLETED, ChangeRequestStatus.FAILED);

    private static final String DEMO_ORIGINAL_CONFIRMATION = "CT-DEMO-78291";
    private static final String DEMO_NEW_CONFIRMATION = "CT-DEMO-89412";
    private static final LocalDate DEMO_PROPOSED_CHECK_IN = LocalDate.of(2027, 3, 15);
    private static final LocalDate DEMO_PROPOSED_CHECK_OUT = LocalDate.of(2027, 3, 20);

    private final BookingRepository bookingRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final BookingEventRepository bookingEventRepository;
    private final HotelSupplierGateway hotelGateway;
    private final CarSupplierGateway carGateway;
    private final TripPricingService pricingService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public RebookOrchestrator(BookingRepository bookingRepository,
                               ChangeRequestRepository changeRequestRepository,
                               BookingEventRepository bookingEventRepository,
                               HotelSupplierGateway hotelGateway,
                               CarSupplierGateway carGateway,
                               TripPricingService pricingService,
                               IdempotencyService idempotencyService,
                               ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.hotelGateway = hotelGateway;
        this.carGateway = carGateway;
        this.pricingService = pricingService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChangeRequestResponse submitChange(String confirmationNumber, ChangeSubmitRequest request, String idempotencyKey) {
        Booking booking = bookingRepository.findWithItemsByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new BookingNotFoundException("No booking found for confirmation " + confirmationNumber));

        // Idempotency replay must be checked before validating the booking's current state:
        // a genuine duplicate of an already-completed request arrives after the booking has
        // already moved on (e.g. CANCELLED), and must still replay the original result rather
        // than fail state validation against the booking's now-different status.
        IdempotencyService.Outcome outcome = idempotencyService.begin(idempotencyKey, "SUBMIT_CHANGE", request);
        if (outcome instanceof IdempotencyService.Replay replay) {
            log.info("Idempotency-Key {} replayed for booking {}", idempotencyKey, confirmationNumber);
            return replay.responseBodyJson() == null ? null : deserialize(replay.responseBodyJson());
        }
        IdempotencyRecord idempotencyRecord = ((IdempotencyService.New) outcome).record();

        try {
            validateOriginal(booking);
        } catch (RuntimeException e) {
            // Don't leave the idempotency record stuck IN_PROGRESS - a retry with the same key
            // should see this same validation failure again, not a false "already in progress".
            idempotencyService.complete(idempotencyRecord, null);
            throw e;
        }

        String correlationId = currentCorrelationId();
        booking.setStatus(BookingStatus.CHANGE_PENDING);
        bookingRepository.saveAndFlush(booking);

        ChangeRequest changeRequest = new ChangeRequest(booking, correlationId, idempotencyKey,
                request.newCheckIn(), request.newCheckOut(), booking.getTotalAmount(), ChangeRequestStatus.REQUESTED);
        changeRequest = changeRequestRepository.save(changeRequest);
        emit(booking.getId(), changeRequest.getId(), correlationId, "CHANGE_REQUESTED",
                "Member requested new dates " + request.newCheckIn() + " to " + request.newCheckOut());

        changeRequest.setStatus(ChangeRequestStatus.VALIDATING);
        emit(booking.getId(), changeRequest.getId(), correlationId, "ORIGINAL_BOOKING_VALIDATED",
                "Original booking " + confirmationNumber + " confirmed valid for change");

        changeRequest.setStatus(ChangeRequestStatus.CHECKING_AVAILABILITY);
        HotelAvailabilityResponse hotelAvailability;
        CarAvailabilityResponse carAvailability;
        try {
            hotelAvailability = hotelGateway.checkAvailability(booking.getDestination(),
                    request.newCheckIn(), request.newCheckOut(), TripConstants.ROOM_TYPE);
            carAvailability = carGateway.checkAvailability(request.newCheckIn(), request.newCheckOut(), TripConstants.CAR_CLASS);
        } catch (SupplierUnavailableException | SupplierTimeoutException e) {
            return preserveOriginal(booking, changeRequest, idempotencyRecord,
                    "AVAILABILITY_CHECK_FAILED", e.getMessage(), ReconciliationStatus.NOT_REQUIRED);
        }

        if (!hotelAvailability.available() || !carAvailability.available()) {
            return preserveOriginal(booking, changeRequest, idempotencyRecord,
                    "NO_AVAILABILITY", "Requested dates are not available from one or more suppliers",
                    ReconciliationStatus.NOT_REQUIRED);
        }

        BigDecimal taxesAndFees = pricingService.taxesAndFeesFor(request.newCheckIn(), request.newCheckOut());
        changeRequest.setStatus(ChangeRequestStatus.REPLACEMENT_PENDING);

        HotelReservationResponse hotelReservation;
        try {
            hotelReservation = reserveHotel(booking, changeRequest, correlationId, request, hotelAvailability);
        } catch (SupplierUnavailableException | SupplierTimeoutException e) {
            return preserveOriginal(booking, changeRequest, idempotencyRecord,
                    "HOTEL_RESERVATION_FAILED", e.getMessage(), ReconciliationStatus.NOT_REQUIRED);
        }
        changeRequest.setStatus(ChangeRequestStatus.HOTEL_RESERVED);

        CarReservationResponse carReservation;
        try {
            carReservation = reserveCar(booking, changeRequest, correlationId, request, carAvailability);
        } catch (SupplierTimeoutException e) {
            // Ambiguous: the car supplier may or may not have actually created the reservation.
            // Compensate the hotel leg now (safe regardless of the car outcome) and require
            // reconciliation to discover and clean up any orphan car reservation.
            emit(booking.getId(), changeRequest.getId(), correlationId, "CAR_RESERVATION_TIMEOUT", e.getMessage());
            compensateHotel(booking, changeRequest, correlationId, hotelReservation);
            return preserveOriginal(booking, changeRequest, idempotencyRecord,
                    "CAR_RESERVATION_AMBIGUOUS",
                    "The car supplier did not respond in time. We could not confirm whether the reservation was created.",
                    ReconciliationStatus.REQUIRED);
        } catch (SupplierUnavailableException e) {
            emit(booking.getId(), changeRequest.getId(), correlationId, "CAR_RESERVATION_FAILED", e.getMessage());
            compensateHotel(booking, changeRequest, correlationId, hotelReservation);
            return preserveOriginal(booking, changeRequest, idempotencyRecord,
                    "CAR_RESERVATION_FAILED", e.getMessage(), ReconciliationStatus.NOT_REQUIRED);
        }
        changeRequest.setStatus(ChangeRequestStatus.CAR_RESERVED);

        return completeReplacement(booking, changeRequest, idempotencyRecord, correlationId, request,
                hotelReservation, carReservation, hotelAvailability.total(), carAvailability.total(), taxesAndFees);
    }

    private void validateOriginal(Booking booking) {
        if (booking.getStatus() == BookingStatus.CHANGE_PENDING) {
            throw new ChangeInProgressException("Another change is already being processed for this reservation.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Booking must be CONFIRMED to request a change (current status: " + booking.getStatus() + ")");
        }
    }

    private HotelReservationResponse reserveHotel(Booking booking, ChangeRequest changeRequest, String correlationId,
                                                   ChangeSubmitRequest request, HotelAvailabilityResponse availability) {
        long start = System.currentTimeMillis();
        try {
            HotelReservationResponse response = hotelGateway.createReservation(new HotelReservationRequest(
                    correlationId + "-HOTEL", booking.getDestination(), request.newCheckIn(), request.newCheckOut(),
                    TripConstants.HOTEL_NAME, TripConstants.ROOM_TYPE, availability.nightlyRate(),
                    availability.taxes(), availability.total()));
            long latency = System.currentTimeMillis() - start;
            emitSupplierCall(booking.getId(), changeRequest.getId(), correlationId, "HOTEL", "SUCCESS",
                    "Hotel replacement reserved: " + response.confirmationNumber(), latency);
            emit(booking.getId(), changeRequest.getId(), correlationId, "HOTEL_RESERVATION_CONFIRMED",
                    "Hotel replacement confirmed: " + response.confirmationNumber());
            changeRequest.addSupplierReservation(new SupplierReservation(SupplierType.HOTEL, "HOTEL_SUPPLIER",
                    response.confirmationNumber(), ReservationStatus.CONFIRMED, availability.total()));
            return response;
        } catch (SupplierUnavailableException | SupplierTimeoutException e) {
            long latency = System.currentTimeMillis() - start;
            emitSupplierCall(booking.getId(), changeRequest.getId(), correlationId, "HOTEL",
                    e instanceof SupplierTimeoutException ? "TIMEOUT" : "FAILURE", e.getMessage(), latency);
            throw e;
        }
    }

    private CarReservationResponse reserveCar(Booking booking, ChangeRequest changeRequest, String correlationId,
                                               ChangeSubmitRequest request, CarAvailabilityResponse availability) {
        long start = System.currentTimeMillis();
        try {
            CarReservationResponse response = carGateway.createReservation(new CarReservationRequest(
                    correlationId + "-CAR", request.newCheckIn(), request.newCheckOut(), TripConstants.CAR_CLASS,
                    availability.dailyRate(), availability.taxes(), availability.total()));
            long latency = System.currentTimeMillis() - start;
            emitSupplierCall(booking.getId(), changeRequest.getId(), correlationId, "CAR", "SUCCESS",
                    "Car replacement reserved: " + response.confirmationNumber(), latency);
            emit(booking.getId(), changeRequest.getId(), correlationId, "CAR_RESERVATION_CONFIRMED",
                    "Car replacement confirmed: " + response.confirmationNumber());
            changeRequest.addSupplierReservation(new SupplierReservation(SupplierType.CAR, "CAR_SUPPLIER",
                    response.confirmationNumber(), ReservationStatus.CONFIRMED, availability.total()));
            return response;
        } catch (SupplierUnavailableException | SupplierTimeoutException e) {
            long latency = System.currentTimeMillis() - start;
            emitSupplierCall(booking.getId(), changeRequest.getId(), correlationId, "CAR",
                    e instanceof SupplierTimeoutException ? "TIMEOUT" : "FAILURE", e.getMessage(), latency);
            throw e;
        }
    }

    private void compensateHotel(Booking booking, ChangeRequest changeRequest, String correlationId,
                                  HotelReservationResponse hotelReservation) {
        changeRequest.setStatus(ChangeRequestStatus.COMPENSATION_PENDING);
        emit(booking.getId(), changeRequest.getId(), correlationId, "COMPENSATION_STARTED",
                "Rolling back the replacement hotel reservation");
        try {
            hotelGateway.cancelReservation(hotelReservation.reservationId());
            changeRequest.getSupplierReservations().stream()
                    .filter(r -> r.getSupplierType() == SupplierType.HOTEL)
                    .findFirst()
                    .ifPresent(r -> r.setStatus(ReservationStatus.CANCELLED));
            emit(booking.getId(), changeRequest.getId(), correlationId, "HOTEL_REPLACEMENT_CANCELLED",
                    "Replacement hotel reservation " + hotelReservation.confirmationNumber() + " cancelled");
        } catch (SupplierUnavailableException e) {
            emit(booking.getId(), changeRequest.getId(), correlationId, "COMPENSATION_FAILED",
                    "Could not cancel replacement hotel reservation: " + e.getMessage());
        }
    }

    /** Reverts the booking to CONFIRMED and returns a member-safe failure response. Used for every failure path. */
    private ChangeRequestResponse preserveOriginal(Booking booking, ChangeRequest changeRequest,
                                                    IdempotencyRecord idempotencyRecord, String eventType,
                                                    String technicalMessage, ReconciliationStatus reconciliationStatus) {
        String correlationId = changeRequest.getCorrelationId();
        emit(booking.getId(), changeRequest.getId(), correlationId, eventType, technicalMessage);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        emit(booking.getId(), changeRequest.getId(), correlationId, "ORIGINAL_BOOKING_PRESERVED",
                "Original booking " + booking.getConfirmationNumber() + " remains confirmed");

        changeRequest.setStatus(ChangeRequestStatus.FAILED);
        changeRequest.setReconciliationStatus(reconciliationStatus);
        changeRequestRepository.save(changeRequest);
        emit(booking.getId(), changeRequest.getId(), correlationId, "CHANGE_FAILED", "Reservation change did not complete");

        ChangeRequestResponse response = new ChangeRequestResponse(
                changeRequest.getId(), correlationId, changeRequest.getStatus().name(),
                changeRequest.getReconciliationStatus().name(), booking.getConfirmationNumber(), null,
                changeRequest.getOldTotal(), null, null,
                "We couldn't complete your requested change",
                "Your existing reservation is still confirmed. Nothing has changed on your current trip.",
                false, eventsFor(correlationId));

        idempotencyService.complete(idempotencyRecord, response);
        return response;
    }

    private ChangeRequestResponse completeReplacement(Booking booking, ChangeRequest changeRequest,
                                                        IdempotencyRecord idempotencyRecord, String correlationId,
                                                        ChangeSubmitRequest request,
                                                        HotelReservationResponse hotelReservation,
                                                        CarReservationResponse carReservation,
                                                        BigDecimal hotelTotal, BigDecimal carTotal, BigDecimal taxesAndFees) {
        BigDecimal newTotal = hotelTotal.add(carTotal).add(taxesAndFees);
        changeRequest.setStatus(ChangeRequestStatus.REPLACEMENT_CONFIRMED);
        changeRequest.setNewTotal(newTotal);
        changeRequest.setPriceDifference(newTotal.subtract(changeRequest.getOldTotal()));
        emit(booking.getId(), changeRequest.getId(), correlationId, "REPLACEMENT_CONFIRMED",
                "Both hotel and car replacements confirmed");

        booking.setStatus(BookingStatus.CANCELLATION_PENDING);
        bookingRepository.save(booking);
        emit(booking.getId(), changeRequest.getId(), correlationId, "ORIGINAL_CANCELLATION_STARTED",
                "Cancelling original reservation " + booking.getConfirmationNumber());

        // The seeded original booking's supplier confirmations were inserted directly into this
        // service's database (V4 seed migration) and were never created via a live call to the
        // mock supplier APIs, so there is no real supplier-side reservation to cancel here. A
        // booking created through this same change flow (e.g. CT-DEMO-89412) always holds live
        // supplier reservation IDs and would be cancelled for real on its own subsequent change.
        emit(booking.getId(), changeRequest.getId(), correlationId, "ORIGINAL_SUPPLIER_RESERVATIONS_CANCELLED",
                "Original supplier reservations released (simulated - original was seeded, not created via a live supplier call)");

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        emit(booking.getId(), changeRequest.getId(), correlationId, "ORIGINAL_BOOKING_CANCELLED",
                "Original booking " + booking.getConfirmationNumber() + " cancelled");

        String newConfirmationNumber = nextConfirmationNumber(booking.getConfirmationNumber(), request);
        Booking newBooking = new Booking(newConfirmationNumber, booking.getMember(), booking.getDestination(),
                request.newCheckIn(), request.newCheckOut(), newTotal, BookingStatus.CONFIRMED);
        newBooking.addItem(new BookingItem("HOTEL", "HOTEL_SUPPLIER",
                TripConstants.HOTEL_NAME + " - " + TripConstants.ROOM_TYPE, hotelTotal,
                hotelReservation.confirmationNumber(), "CONFIRMED"));
        newBooking.addItem(new BookingItem("CAR", "CAR_SUPPLIER", TripConstants.CAR_CLASS, carTotal,
                carReservation.confirmationNumber(), "CONFIRMED"));
        newBooking.addItem(new BookingItem("FEES", "INTERNAL", "Taxes & Fees", taxesAndFees, null, "CONFIRMED"));
        newBooking.addItem(new BookingItem("MEMBER_BENEFIT", "INTERNAL", "$200 Digital Costco Shop Card",
                BigDecimal.ZERO, null, "CONFIRMED"));
        bookingRepository.save(newBooking);
        emit(newBooking.getId(), changeRequest.getId(), correlationId, "NEW_BOOKING_CONFIRMED",
                "New booking " + newConfirmationNumber + " confirmed");

        changeRequest.setStatus(ChangeRequestStatus.COMPLETED);
        changeRequestRepository.save(changeRequest);
        emit(booking.getId(), changeRequest.getId(), correlationId, "CHANGE_COMPLETED", "Reservation change completed successfully");

        ChangeRequestResponse response = new ChangeRequestResponse(
                changeRequest.getId(), correlationId, changeRequest.getStatus().name(),
                changeRequest.getReconciliationStatus().name(), booking.getConfirmationNumber(), newConfirmationNumber,
                changeRequest.getOldTotal(), newTotal, changeRequest.getPriceDifference(),
                "Your trip has been successfully updated",
                "Original: " + booking.getConfirmationNumber() + " - Cancelled. New: " + newConfirmationNumber + " - Confirmed.",
                true, eventsFor(correlationId));

        idempotencyService.complete(idempotencyRecord, response);
        return response;
    }

    private String nextConfirmationNumber(String oldConfirmationNumber, ChangeSubmitRequest request) {
        if (DEMO_ORIGINAL_CONFIRMATION.equals(oldConfirmationNumber)
                && DEMO_PROPOSED_CHECK_IN.equals(request.newCheckIn())
                && DEMO_PROPOSED_CHECK_OUT.equals(request.newCheckOut())
                && bookingRepository.findByConfirmationNumber(DEMO_NEW_CONFIRMATION).isEmpty()) {
            return DEMO_NEW_CONFIRMATION;
        }
        return "CT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String currentCorrelationId() {
        String id = MDC.get(CorrelationIdFilter.MDC_KEY);
        return id != null ? id : CorrelationIdFilter.generate();
    }

    private void emit(Long bookingId, Long changeRequestId, String correlationId, String eventType, String message) {
        bookingEventRepository.save(BookingEvent.lifecycle(bookingId, changeRequestId, correlationId, eventType, message));
    }

    private void emitSupplierCall(Long bookingId, Long changeRequestId, String correlationId, String supplier,
                                   String status, String message, long latencyMs) {
        bookingEventRepository.save(
                BookingEvent.supplierCall(bookingId, changeRequestId, correlationId, supplier, status, message, latencyMs));
    }

    private List<BookingEventDto> eventsFor(String correlationId) {
        return bookingEventRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId).stream()
                .map(e -> new BookingEventDto(e.getEventType(), e.getSupplier(), e.getStatus(), e.getMessage(),
                        e.getLatencyMs(), e.getCreatedAt()))
                .toList();
    }

    private ChangeRequestResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ChangeRequestResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize replayed idempotent response", e);
        }
    }
}
