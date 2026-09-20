package com.smartrebook.booking.dto;

import java.time.Instant;

public record TransactionSummaryDto(
        Long changeRequestId,
        String correlationId,
        String bookingConfirmation,
        String memberName,
        String operation,
        String status,
        String reconciliationStatus,
        Instant startedAt,
        Long durationMs
) {
}
