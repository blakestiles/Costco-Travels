package com.smartrebook.contracts.hotel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HotelReservationRequest(
        @NotBlank String clientReference,
        @NotBlank String destination,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotBlank String hotel,
        @NotBlank String room,
        @NotNull BigDecimal nightlyRate,
        @NotNull BigDecimal taxes,
        @NotNull BigDecimal total
) {
}
