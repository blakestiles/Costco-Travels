package com.smartrebook.booking.repository.jdbc;

import java.math.BigDecimal;

public record SupplierReliabilityRow(
        String supplierName,
        long bookingAttempts,
        long successCount,
        long failureCount,
        BigDecimal successRate,
        long averageLatencyMs,
        long p95LatencyMs
) {
}
