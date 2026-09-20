package com.smartrebook.booking.dto;

import java.math.BigDecimal;

public record BookingItemDto(
        String itemType,
        String supplier,
        String description,
        BigDecimal amount,
        String supplierConfirmation,
        String status
) {
}
