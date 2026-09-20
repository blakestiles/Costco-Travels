package com.smartrebook.booking.service;

import com.smartrebook.booking.domain.ChangeRequest;
import com.smartrebook.booking.dto.BookingEventDto;
import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.dto.TransactionSummaryDto;
import com.smartrebook.booking.exception.InvalidBookingStateException;
import com.smartrebook.booking.repository.BookingEventRepository;
import com.smartrebook.booking.repository.ChangeRequestRepository;
import com.smartrebook.booking.repository.jdbc.SupplierReliabilityJdbcRepository;
import com.smartrebook.booking.repository.jdbc.SupplierReliabilityRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class OpsQueryService {

    private final ChangeRequestRepository changeRequestRepository;
    private final BookingEventRepository bookingEventRepository;
    private final SupplierReliabilityJdbcRepository reliabilityJdbcRepository;

    public OpsQueryService(ChangeRequestRepository changeRequestRepository,
                            BookingEventRepository bookingEventRepository,
                            SupplierReliabilityJdbcRepository reliabilityJdbcRepository) {
        this.changeRequestRepository = changeRequestRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.reliabilityJdbcRepository = reliabilityJdbcRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionSummaryDto> listTransactions() {
        return changeRequestRepository.findAllWithBookingOrderByCreatedAtDesc().stream()
                .map(cr -> new TransactionSummaryDto(
                        cr.getId(), cr.getCorrelationId(), cr.getBooking().getConfirmationNumber(),
                        cr.getBooking().getMember().getFirstName() + " " + cr.getBooking().getMember().getLastName(),
                        "REBOOK_CHANGE", cr.getStatus().name(), cr.getReconciliationStatus().name(),
                        cr.getCreatedAt(), Duration.between(cr.getCreatedAt(), cr.getUpdatedAt()).toMillis()
                ))
                .toList();
    }

    /** The timeline shown on the transaction-detail page is rendered from these persisted rows - never hardcoded. */
    @Transactional(readOnly = true)
    public ChangeRequestResponse getTransactionDetail(String correlationId) {
        ChangeRequest cr = changeRequestRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new InvalidBookingStateException("No transaction found for correlation " + correlationId));

        List<BookingEventDto> events = bookingEventRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId).stream()
                .map(e -> new BookingEventDto(e.getEventType(), e.getSupplier(), e.getStatus(), e.getMessage(),
                        e.getLatencyMs(), e.getCreatedAt()))
                .toList();

        boolean success = "COMPLETED".equals(cr.getStatus().name());
        return new ChangeRequestResponse(
                cr.getId(), cr.getCorrelationId(), cr.getStatus().name(), cr.getReconciliationStatus().name(),
                cr.getBooking().getConfirmationNumber(), null, cr.getOldTotal(), cr.getNewTotal(), cr.getPriceDifference(),
                success ? "Your trip has been successfully updated" : "We couldn't complete your requested change",
                success ? "Change completed" : "Original reservation preserved", success, events
        );
    }

    @Transactional(readOnly = true)
    public ChangeRequestResponse getByChangeRequestId(Long id) {
        ChangeRequest cr = changeRequestRepository.findWithDetailsById(id)
                .orElseThrow(() -> new InvalidBookingStateException("Change request not found: " + id));
        return getTransactionDetail(cr.getCorrelationId());
    }

    @Transactional(readOnly = true)
    public List<SupplierReliabilityRow> supplierReliability() {
        return reliabilityJdbcRepository.fetchReliabilityReport();
    }
}
