package com.smartrebook.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ChangeAvailabilityRequest(
        @NotNull LocalDate newCheckIn,
        @NotNull LocalDate newCheckOut
) {
}
