package com.smartrebook.contracts.hotel;

import java.math.BigDecimal;

public record HotelAvailabilityResponse(
        boolean available,
        String hotel,
        String room,
        BigDecimal nightlyRate,
        BigDecimal taxes,
        BigDecimal total,
        String memberExtras
) {
}
