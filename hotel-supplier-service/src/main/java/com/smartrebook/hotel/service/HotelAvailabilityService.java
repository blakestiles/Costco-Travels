package com.smartrebook.hotel.service;

import com.smartrebook.contracts.hotel.HotelAvailabilityRequest;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class HotelAvailabilityService {

    private static final String DEFAULT_HOTEL = "Wailea Beach Resort";
    private static final String MEMBER_EXTRAS = "$200 Digital Costco Shop Card";
    private static final BigDecimal DEFAULT_NIGHTLY_RATE = new BigDecimal("356.00");
    private static final BigDecimal ZERO_TAXES = new BigDecimal("0.00");

    private static final String MAUI = "Maui, Hawaii";
    private static final String OCEAN_VIEW_KING = "Ocean View King";

    public HotelAvailabilityResponse checkAvailability(HotelAvailabilityRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            return new HotelAvailabilityResponse(false, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, null);
        }

        if (isScenario(request, LocalDate.of(2027, 3, 14), LocalDate.of(2027, 3, 19))) {
            return new HotelAvailabilityResponse(true, DEFAULT_HOTEL, OCEAN_VIEW_KING,
                    new BigDecimal("356.00"), ZERO_TAXES, new BigDecimal("1780.00"), MEMBER_EXTRAS);
        }

        if (isScenario(request, LocalDate.of(2027, 3, 15), LocalDate.of(2027, 3, 20))) {
            return new HotelAvailabilityResponse(true, DEFAULT_HOTEL, OCEAN_VIEW_KING,
                    new BigDecimal("364.00"), ZERO_TAXES, new BigDecimal("1820.00"), MEMBER_EXTRAS);
        }

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        BigDecimal total = DEFAULT_NIGHTLY_RATE.multiply(BigDecimal.valueOf(nights));
        return new HotelAvailabilityResponse(nights > 0, DEFAULT_HOTEL, request.roomType(),
                DEFAULT_NIGHTLY_RATE, ZERO_TAXES, total, MEMBER_EXTRAS);
    }

    private boolean isScenario(HotelAvailabilityRequest request, LocalDate checkIn, LocalDate checkOut) {
        return MAUI.equals(request.destination())
                && OCEAN_VIEW_KING.equals(request.roomType())
                && checkIn.equals(request.checkIn())
                && checkOut.equals(request.checkOut());
    }
}
