package com.smartrebook.contracts.hotel;

import com.smartrebook.contracts.ReservationStatus;

import java.math.BigDecimal;

public record HotelReservationResponse(
        String reservationId,
        String clientReference,
        ReservationStatus status,
        String confirmationNumber,
        BigDecimal total
) {
}
