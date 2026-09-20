package com.smartrebook.contracts.car;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarReservationRequest(
        @NotBlank String clientReference,
        @NotNull LocalDate pickupDate,
        @NotNull LocalDate returnDate,
        @NotBlank String carClass,
        @NotNull BigDecimal dailyRate,
        @NotNull BigDecimal taxes,
        @NotNull BigDecimal total
) {
}
