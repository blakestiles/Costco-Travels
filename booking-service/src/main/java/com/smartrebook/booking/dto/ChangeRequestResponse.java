package com.smartrebook.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public record ChangeRequestResponse(
        Long id,
        String correlationId,
        String status,
        String reconciliationStatus,
        String oldConfirmationNumber,
        String newConfirmationNumber,
        BigDecimal oldTotal,
        BigDecimal newTotal,
        BigDecimal priceDifference,
        String memberHeadline,
        String memberDetail,
        boolean success,
        List<BookingEventDto> events
) {
}
