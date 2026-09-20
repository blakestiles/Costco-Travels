package com.smartrebook.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingSummaryDto(
        String confirmationNumber,
        String destination,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String status,
        BigDecimal totalAmount
) {
}
