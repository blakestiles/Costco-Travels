package com.smartrebook.contracts.car;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CarAvailabilityRequest(
        @NotNull LocalDate pickupDate,
        @NotNull LocalDate returnDate,
        @NotBlank String carClass
) {
}
