package com.smartrebook.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        String confirmationNumber,
        String destination,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String status,
        BigDecimal totalAmount,
        String currency,
        String memberName,
        String membershipType,
        List<BookingItemDto> items
) {
}
