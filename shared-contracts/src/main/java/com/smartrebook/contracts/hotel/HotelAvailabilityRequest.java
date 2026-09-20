package com.smartrebook.contracts.hotel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HotelAvailabilityRequest(
        @NotBlank String destination,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotBlank String roomType
) {
}
