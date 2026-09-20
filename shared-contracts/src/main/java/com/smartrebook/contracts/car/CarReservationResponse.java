package com.smartrebook.contracts.car;

import com.smartrebook.contracts.ReservationStatus;

import java.math.BigDecimal;

public record CarReservationResponse(
        String reservationId,
        String clientReference,
        ReservationStatus status,
        String confirmationNumber,
        BigDecimal total
) {
}
