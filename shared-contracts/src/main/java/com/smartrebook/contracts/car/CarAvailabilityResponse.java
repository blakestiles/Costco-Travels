package com.smartrebook.contracts.car;

import java.math.BigDecimal;

public record CarAvailabilityResponse(
        boolean available,
        String carClass,
        BigDecimal dailyRate,
        BigDecimal taxes,
        BigDecimal total
) {
}
