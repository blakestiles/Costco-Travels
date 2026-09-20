package com.smartrebook.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripSummary(
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal hotelTotal,
        BigDecimal carTotal,
        BigDecimal taxesAndFees,
        BigDecimal total
) {
}
