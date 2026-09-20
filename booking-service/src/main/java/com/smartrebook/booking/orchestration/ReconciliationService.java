package com.smartrebook.booking.orchestration;

import com.smartrebook.booking.client.CarSupplierGateway;
import com.smartrebook.booking.domain.*;
import com.smartrebook.booking.dto.BookingEventDto;
import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.exception.InvalidBookingStateException;
import com.smartrebook.booking.repository.BookingEventRepository;
import com.smartrebook.booking.repository.ChangeRequestRepository;
import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.contracts.car.CarReservationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves a RECONCILIATION_REQUIRED change request by asking the car supplier directly whether
 * it actually created the reservation for our client reference. Timeout was never treated as a
 * confirmed failure at the time it happened (see RebookOrchestrator) - this is where that
 * ambiguity gets safely resolved, one way or the other, without ever guessing.
 */
@Service
public class ReconciliationService {

    private final ChangeRequestRepository changeRequestRepository;
    private final BookingEventRepository bookingEventRepository;
    private final CarSupplierGateway carGateway;

    public ReconciliationService(ChangeRequestRepository changeRequestRepository,
                                  BookingEventRepository bookingEventRepository,
                                  CarSupplierGateway carGateway) {
        this.changeRequestRepository = changeRequestRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.carGateway = carGateway;
    }

    @Transactional
    public ChangeRequestResponse reconcile(Long changeRequestId) {
        ChangeRequest changeRequest = changeRequestRepository.findWithDetailsById(changeRequestId)
                .orElseThrow(() -> new InvalidBookingStateException("Change request not found: " + changeRequestId));

        if (changeRequest.getReconciliationStatus() != ReconciliationStatus.REQUIRED) {
            throw new InvalidBookingStateException(
                    "Change request " + changeRequestId + " does not require reconciliation (status: "
                            + changeRequest.getReconciliationStatus() + ")");
        }

        String correlationId = changeRequest.getCorrelationId();
        Booking booking = changeRequest.getBooking();
        changeRequest.setReconciliationStatus(ReconciliationStatus.IN_PROGRESS);
        emit(booking.getId(), changeRequest.getId(), correlationId, "RECONCILIATION_STARTED",
                "Checking car supplier for an orphaned reservation");

        String clientReference = correlationId + "-CAR";
        Optional<CarReservationResponse> orphan = carGateway.findByClientReference(clientReference);

        if (orphan.isPresent()) {
            CarReservationResponse reservation = orphan.get();
            carGateway.cancelReservation(reservation.reservationId());
            changeRequest.addSupplierReservation(new SupplierReservation(SupplierType.CAR, "CAR_SUPPLIER",
                    reservation.confirmationNumber(), ReservationStatus.CANCELLED, reservation.total()));
            emit(booking.getId(), changeRequest.getId(), correlationId, "ORPHAN_CAR_RESERVATION_CANCELLED",
                    "Car supplier had created reservation " + reservation.confirmationNumber()
                            + " after our client timeout; it has been cancelled");
        } else {
            emit(booking.getId(), changeRequest.getId(), correlationId, "RECONCILIATION_NO_ORPHAN_FOUND",
                    "Car supplier confirms no reservation was created for this change request");
        }

        changeRequest.setReconciliationStatus(ReconciliationStatus.RESOLVED);
        changeRequestRepository.save(changeRequest);
        emit(booking.getId(), changeRequest.getId(), correlationId, "RECONCILIATION_COMPLETED",
                "Reconciliation resolved; original booking " + booking.getConfirmationNumber() + " remains confirmed");

        return new ChangeRequestResponse(
                changeRequest.getId(), correlationId, changeRequest.getStatus().name(),
                changeRequest.getReconciliationStatus().name(), booking.getConfirmationNumber(), null,
                changeRequest.getOldTotal(), null, null,
                "We couldn't complete your requested change",
                "Your existing reservation is still confirmed. Nothing has changed on your current trip.",
                false, eventsFor(correlationId));
    }

    private void emit(Long bookingId, Long changeRequestId, String correlationId, String eventType, String message) {
        bookingEventRepository.save(BookingEvent.lifecycle(bookingId, changeRequestId, correlationId, eventType, message));
    }

    private java.util.List<BookingEventDto> eventsFor(String correlationId) {
        return bookingEventRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId).stream()
                .map(e -> new BookingEventDto(e.getEventType(), e.getSupplier(), e.getStatus(), e.getMessage(),
                        e.getLatencyMs(), e.getCreatedAt()))
                .toList();
    }
}
