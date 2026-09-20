package com.smartrebook.booking.orchestration;

import com.smartrebook.booking.client.CarSupplierGateway;
import com.smartrebook.booking.domain.Booking;
import com.smartrebook.booking.domain.BookingStatus;
import com.smartrebook.booking.domain.ChangeRequest;
import com.smartrebook.booking.domain.ChangeRequestStatus;
import com.smartrebook.booking.domain.Member;
import com.smartrebook.booking.domain.ReconciliationStatus;
import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.exception.InvalidBookingStateException;
import com.smartrebook.booking.repository.BookingEventRepository;
import com.smartrebook.booking.repository.ChangeRequestRepository;
import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.contracts.car.CarReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;
    @Mock
    private BookingEventRepository bookingEventRepository;
    @Mock
    private CarSupplierGateway carGateway;

    private ReconciliationService reconciliationService;

    private static final Long CHANGE_REQUEST_ID = 7L;
    private static final String CORRELATION_ID = "CHG-TEST01";

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(changeRequestRepository, bookingEventRepository, carGateway);
        lenient().when(bookingEventRepository.findByCorrelationIdOrderByCreatedAtAsc(anyString())).thenReturn(List.of());
    }

    private ChangeRequest changeRequestRequiringReconciliation() {
        Member member = new Member("M0001", "Jane", "Doe", "Executive");
        Booking booking = new Booking("CT-TEST0001", member, "Maui, Hawaii", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), new BigDecimal("1500.00"), BookingStatus.CONFIRMED);
        ChangeRequest changeRequest = new ChangeRequest(booking, CORRELATION_ID, "IDEMP-KEY-1",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), new BigDecimal("1500.00"),
                ChangeRequestStatus.FAILED);
        changeRequest.setReconciliationStatus(ReconciliationStatus.REQUIRED);
        return changeRequest;
    }

    // 14. Orphan car reservation found -> cancels it and resolves
    @Test
    void reconcile_orphanFound_cancelsReservationAndResolves() {
        ChangeRequest changeRequest = changeRequestRequiringReconciliation();
        when(changeRequestRepository.findWithDetailsById(CHANGE_REQUEST_ID)).thenReturn(Optional.of(changeRequest));

        CarReservationResponse orphan = new CarReservationResponse("CRES-ORPHAN", CORRELATION_ID + "-CAR",
                ReservationStatus.CONFIRMED, "CCONF-ORPHAN", new BigDecimal("420.00"));
        when(carGateway.findByClientReference(CORRELATION_ID + "-CAR")).thenReturn(Optional.of(orphan));

        ChangeRequestResponse response = reconciliationService.reconcile(CHANGE_REQUEST_ID);

        verify(carGateway).cancelReservation("CRES-ORPHAN");
        assertEquals(ReconciliationStatus.RESOLVED, changeRequest.getReconciliationStatus());
        assertFalse(response.success());

        ArgumentCaptor<ChangeRequest> captor = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertEquals(ReconciliationStatus.RESOLVED, captor.getValue().getReconciliationStatus());
    }

    // 15. No orphan found -> resolves without cancelling
    @Test
    void reconcile_noOrphanFound_resolvesWithoutCancelling() {
        ChangeRequest changeRequest = changeRequestRequiringReconciliation();
        when(changeRequestRepository.findWithDetailsById(CHANGE_REQUEST_ID)).thenReturn(Optional.of(changeRequest));
        when(carGateway.findByClientReference(CORRELATION_ID + "-CAR")).thenReturn(Optional.empty());

        reconciliationService.reconcile(CHANGE_REQUEST_ID);

        verify(carGateway, never()).cancelReservation(any());
        assertEquals(ReconciliationStatus.RESOLVED, changeRequest.getReconciliationStatus());
    }

    // 16. Reconciliation not required -> throws InvalidBookingStateException
    @Test
    void reconcile_notRequired_throwsInvalidBookingStateException() {
        ChangeRequest changeRequest = changeRequestRequiringReconciliation();
        changeRequest.setReconciliationStatus(ReconciliationStatus.NOT_REQUIRED);
        when(changeRequestRepository.findWithDetailsById(CHANGE_REQUEST_ID)).thenReturn(Optional.of(changeRequest));

        assertThrows(InvalidBookingStateException.class, () -> reconciliationService.reconcile(CHANGE_REQUEST_ID));

        verify(carGateway, never()).findByClientReference(any());
        verify(changeRequestRepository, never()).save(any());
    }
}
