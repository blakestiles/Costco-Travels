package com.smartrebook.booking.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartrebook.booking.client.CarSupplierGateway;
import com.smartrebook.booking.client.HotelSupplierGateway;
import com.smartrebook.booking.domain.Booking;
import com.smartrebook.booking.domain.BookingStatus;
import com.smartrebook.booking.domain.ChangeRequest;
import com.smartrebook.booking.domain.ChangeRequestStatus;
import com.smartrebook.booking.domain.IdempotencyRecord;
import com.smartrebook.booking.domain.IdempotencyStatus;
import com.smartrebook.booking.domain.Member;
import com.smartrebook.booking.domain.ReconciliationStatus;
import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.dto.ChangeSubmitRequest;
import com.smartrebook.booking.exception.ChangeInProgressException;
import com.smartrebook.booking.exception.IdempotencyConflictException;
import com.smartrebook.booking.exception.InvalidBookingStateException;
import com.smartrebook.booking.exception.SupplierTimeoutException;
import com.smartrebook.booking.exception.SupplierUnavailableException;
import com.smartrebook.booking.repository.BookingEventRepository;
import com.smartrebook.booking.repository.BookingRepository;
import com.smartrebook.booking.repository.ChangeRequestRepository;
import com.smartrebook.booking.service.IdempotencyService;
import com.smartrebook.booking.service.TripPricingService;
import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.car.CarReservationResponse;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RebookOrchestratorTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ChangeRequestRepository changeRequestRepository;
    @Mock
    private BookingEventRepository bookingEventRepository;
    @Mock
    private HotelSupplierGateway hotelGateway;
    @Mock
    private CarSupplierGateway carGateway;
    @Mock
    private TripPricingService pricingService;
    @Mock
    private IdempotencyService idempotencyService;

    private ObjectMapper objectMapper;
    private RebookOrchestrator orchestrator;

    private static final String CONFIRMATION_NUMBER = "CT-TEST0001";
    private static final LocalDate NEW_CHECK_IN = LocalDate.of(2026, 7, 1);
    private static final LocalDate NEW_CHECK_OUT = LocalDate.of(2026, 7, 5);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        orchestrator = new RebookOrchestrator(bookingRepository, changeRequestRepository, bookingEventRepository,
                hotelGateway, carGateway, pricingService, idempotencyService, objectMapper);

        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(changeRequestRepository.save(any(ChangeRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(bookingEventRepository.findByCorrelationIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());
    }

    private Booking confirmedBooking() {
        Member member = new Member("M0001", "Jane", "Doe", "Executive");
        return new Booking(CONFIRMATION_NUMBER, member, "Maui, Hawaii", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), new BigDecimal("1500.00"), BookingStatus.CONFIRMED);
    }

    private void stubIdempotencyBeginNew(IdempotencyRecord record) {
        when(idempotencyService.begin(anyString(), eq("SUBMIT_CHANGE"), any()))
                .thenReturn(new IdempotencyService.New(record));
    }

    private IdempotencyRecord newIdempotencyRecord() {
        return new IdempotencyRecord("IDEMP-KEY-1", "hash", "SUBMIT_CHANGE", IdempotencyStatus.IN_PROGRESS);
    }

    private HotelAvailabilityResponse hotelAvailable() {
        return new HotelAvailabilityResponse(true, "Wailea Beach Resort", "Ocean View King",
                new BigDecimal("300.00"), new BigDecimal("50.00"), new BigDecimal("1550.00"), null);
    }

    private CarAvailabilityResponse carAvailable() {
        return new CarAvailabilityResponse(true, "Standard SUV", new BigDecimal("80.00"),
                new BigDecimal("20.00"), new BigDecimal("420.00"));
    }

    private HotelReservationResponse hotelReservation() {
        return new HotelReservationResponse("HRES-1", "corr-HOTEL", ReservationStatus.CONFIRMED, "HCONF-1",
                new BigDecimal("1550.00"));
    }

    private CarReservationResponse carReservation() {
        return new CarReservationResponse("CRES-1", "corr-CAR", ReservationStatus.CONFIRMED, "CCONF-1",
                new BigDecimal("420.00"));
    }

    // 1. Successful booking replacement
    @Test
    void submitChange_bothSuppliersSucceed_replacesBookingAndCompletesChangeRequest() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());
        when(hotelGateway.checkAvailability(any(), any(), any(), any())).thenReturn(hotelAvailable());
        when(carGateway.checkAvailability(any(), any(), any())).thenReturn(carAvailable());
        when(pricingService.taxesAndFeesFor(NEW_CHECK_IN, NEW_CHECK_OUT)).thenReturn(new BigDecimal("200.00"));
        when(hotelGateway.createReservation(any())).thenReturn(hotelReservation());
        when(carGateway.createReservation(any())).thenReturn(carReservation());

        ChangeRequestResponse response = orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1");

        assertTrue(response.success());
        assertEquals(CONFIRMATION_NUMBER, response.oldConfirmationNumber());
        assertThat(response.newConfirmationNumber()).isNotNull().startsWith("CT-");
        assertEquals(new BigDecimal("2170.00"), response.newTotal());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(3)).save(bookingCaptor.capture());
        List<Booking> savedBookings = bookingCaptor.getAllValues();
        Booking savedOriginal = savedBookings.stream()
                .filter(b -> CONFIRMATION_NUMBER.equals(b.getConfirmationNumber())).reduce((a, b) -> b).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, savedOriginal.getStatus());
        Booking savedNew = savedBookings.stream()
                .filter(b -> !CONFIRMATION_NUMBER.equals(b.getConfirmationNumber())).findFirst().orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, savedNew.getStatus());

        ArgumentCaptor<ChangeRequest> crCaptor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository, times(2)).save(crCaptor.capture());
        assertEquals(ChangeRequestStatus.COMPLETED, crCaptor.getValue().getStatus());

        verify(idempotencyService).complete(any(IdempotencyRecord.class), any(ChangeRequestResponse.class));
    }

    // 2. Hotel reservation failure
    @Test
    void submitChange_hotelReservationFails_preservesOriginalAndNeverCallsCar() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());
        when(hotelGateway.checkAvailability(any(), any(), any(), any())).thenReturn(hotelAvailable());
        when(carGateway.checkAvailability(any(), any(), any())).thenReturn(carAvailable());
        when(pricingService.taxesAndFeesFor(any(), any())).thenReturn(new BigDecimal("200.00"));
        when(hotelGateway.createReservation(any()))
                .thenThrow(new SupplierUnavailableException("HOTEL", "Hotel supplier is unavailable", null));

        ChangeRequestResponse response = orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1");

        assertFalse(response.success());
        verify(carGateway, never()).createReservation(any());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(BookingStatus.CONFIRMED, bookingCaptor.getValue().getStatus());

        ArgumentCaptor<ChangeRequest> crCaptor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository, times(2)).save(crCaptor.capture());
        assertEquals(ReconciliationStatus.NOT_REQUIRED, crCaptor.getValue().getReconciliationStatus());
        assertEquals(ChangeRequestStatus.FAILED, crCaptor.getValue().getStatus());
    }

    // 3. Car timeout with compensation
    @Test
    void submitChange_carTimeoutAfterHotelSucceeds_compensatesHotelAndRequiresReconciliation() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());
        when(hotelGateway.checkAvailability(any(), any(), any(), any())).thenReturn(hotelAvailable());
        when(carGateway.checkAvailability(any(), any(), any())).thenReturn(carAvailable());
        when(pricingService.taxesAndFeesFor(any(), any())).thenReturn(new BigDecimal("200.00"));
        HotelReservationResponse hotelRes = hotelReservation();
        when(hotelGateway.createReservation(any())).thenReturn(hotelRes);
        when(carGateway.createReservation(any()))
                .thenThrow(new SupplierTimeoutException("CAR", "Car reservation request timed out", null));

        ChangeRequestResponse response = orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1");

        assertFalse(response.success());
        verify(hotelGateway).cancelReservation(hotelRes.reservationId());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(BookingStatus.CONFIRMED, bookingCaptor.getValue().getStatus());

        ArgumentCaptor<ChangeRequest> crCaptor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository, times(2)).save(crCaptor.capture());
        assertEquals(ReconciliationStatus.REQUIRED, crCaptor.getValue().getReconciliationStatus());
    }

    // 5. Idempotent duplicate request
    @Test
    void submitChange_idempotentReplay_returnsStoredResponseWithoutCallingGateways() throws Exception {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));

        ChangeRequestResponse storedResponse = new ChangeRequestResponse(
                42L, "CHG-STORED1", "COMPLETED", "NOT_REQUIRED", CONFIRMATION_NUMBER, "CT-REPLAYED",
                new BigDecimal("1500.00"), new BigDecimal("2000.00"), new BigDecimal("500.00"),
                "Your trip has been successfully updated", "detail", true, List.of());
        String json = objectMapper.writeValueAsString(storedResponse);
        when(idempotencyService.begin(anyString(), eq("SUBMIT_CHANGE"), any()))
                .thenReturn(new IdempotencyService.Replay(json));

        ChangeRequestResponse response = orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1");

        assertEquals(storedResponse, response);
        verify(hotelGateway, never()).checkAvailability(any(), any(), any(), any());
        verify(carGateway, never()).checkAvailability(any(), any(), any());
        verify(hotelGateway, never()).createReservation(any());
        verify(carGateway, never()).createReservation(any());
        verify(bookingRepository, never()).save(any());
    }

    // 6. Idempotency conflict propagation
    @Test
    void submitChange_idempotencyConflict_propagatesException() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        when(idempotencyService.begin(anyString(), eq("SUBMIT_CHANGE"), any()))
                .thenThrow(new IdempotencyConflictException("Idempotency-Key already used with a different body"));

        assertThrows(IdempotencyConflictException.class, () -> orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1"));

        verify(hotelGateway, never()).checkAvailability(any(), any(), any(), any());
        verify(carGateway, never()).checkAvailability(any(), any(), any());
    }

    // 7. Ambiguous supplier timeout -> reconciliation required (explicit enum check, duplicate scenario of #3)
    @Test
    void submitChange_carTimeout_setsReconciliationStatusRequiredExactly() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());
        when(hotelGateway.checkAvailability(any(), any(), any(), any())).thenReturn(hotelAvailable());
        when(carGateway.checkAvailability(any(), any(), any())).thenReturn(carAvailable());
        when(pricingService.taxesAndFeesFor(any(), any())).thenReturn(new BigDecimal("200.00"));
        when(hotelGateway.createReservation(any())).thenReturn(hotelReservation());
        when(carGateway.createReservation(any()))
                .thenThrow(new SupplierTimeoutException("CAR", "Car reservation request timed out", null));

        ChangeRequestResponse response = orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1");

        assertEquals(ReconciliationStatus.REQUIRED.name(), response.reconciliationStatus());
    }

    // 8a. Invalid original booking state (CANCELLED)
    @Test
    void submitChange_bookingNotConfirmed_throwsInvalidBookingStateExceptionWithoutGatewayCalls() {
        Member member = new Member("M0001", "Jane", "Doe", "Executive");
        Booking booking = new Booking(CONFIRMATION_NUMBER, member, "Maui, Hawaii", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), new BigDecimal("1500.00"), BookingStatus.CANCELLED);
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());

        assertThrows(InvalidBookingStateException.class, () -> orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1"));

        verify(hotelGateway, never()).checkAvailability(any(), any(), any(), any());
        verify(carGateway, never()).checkAvailability(any(), any(), any());
        verify(idempotencyService).complete(any(IdempotencyRecord.class), eq(null));
    }

    // 8b/9. Change already in progress (CHANGE_PENDING) -> specific exception
    @Test
    void submitChange_bookingChangePending_throwsChangeInProgressException() {
        Member member = new Member("M0001", "Jane", "Doe", "Executive");
        Booking booking = new Booking(CONFIRMATION_NUMBER, member, "Maui, Hawaii", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), new BigDecimal("1500.00"), BookingStatus.CHANGE_PENDING);
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());

        assertThrows(ChangeInProgressException.class, () -> orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1"));

        verify(hotelGateway, never()).checkAvailability(any(), any(), any(), any());
    }

    // 9. Optimistic locking failure on saveAndFlush propagates uncaught
    @Test
    void submitChange_optimisticLockingFailureOnSaveAndFlush_propagates() {
        Booking booking = confirmedBooking();
        when(bookingRepository.findWithItemsByConfirmationNumber(CONFIRMATION_NUMBER)).thenReturn(Optional.of(booking));
        stubIdempotencyBeginNew(newIdempotencyRecord());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Booking.class, CONFIRMATION_NUMBER));

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> orchestrator.submitChange(CONFIRMATION_NUMBER,
                new ChangeSubmitRequest(NEW_CHECK_IN, NEW_CHECK_OUT), "IDEMP-KEY-1"));

        verify(hotelGateway, never()).checkAvailability(any(), any(), any(), any());
    }
}
