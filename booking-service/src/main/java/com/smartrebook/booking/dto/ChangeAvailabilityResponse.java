package com.smartrebook.booking.dto;

import java.math.BigDecimal;

public record ChangeAvailabilityResponse(
        TripSummary current,
        TripSummary proposed,
        BigDecimal priceDifference,
        boolean hotelAvailable,
        boolean carAvailable,
        String memberBenefitNote
) {
}
