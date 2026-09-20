package com.smartrebook.booking.service;

import com.smartrebook.booking.client.CarSupplierGateway;
import com.smartrebook.booking.client.HotelSupplierGateway;
import com.smartrebook.booking.domain.Booking;
import com.smartrebook.booking.domain.BookingStatus;
import com.smartrebook.booking.domain.Member;
import com.smartrebook.booking.dto.ChangeAvailabilityResponse;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripPricingServiceTest {

    @Mock
    private HotelSupplierGateway hotelGateway;
    @Mock
    private CarSupplierGateway carGateway;

    private TripPricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new TripPricingService(hotelGateway, carGateway);
    }

    // 10. Price comparison calculation
    @Test
    void compareForProposedDates_computesProposedTotalAndPriceDifference() {
        Member member = new Member("M0001", "Jane", "Doe", "Executive");
        Booking booking = new Booking("CT-TEST0001", member, "Maui, Hawaii", LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5), new BigDecimal("1500.00"), BookingStatus.CONFIRMED);

        LocalDate newCheckIn = LocalDate.of(2026, 7, 1);
        LocalDate newCheckOut = LocalDate.of(2026, 7, 5);

        when(hotelGateway.checkAvailability(any(), any(), any(), any())).thenReturn(
                new HotelAvailabilityResponse(true, "Wailea Beach Resort", "Ocean View King",
                        new BigDecimal("110.00"), new BigDecimal("0.00"), new BigDecimal("550.00"), null));
        when(carGateway.checkAvailability(any(), any(), any())).thenReturn(
                new CarAvailabilityResponse(true, "Standard SUV", new BigDecimal("84.00"),
                        new BigDecimal("0.00"), new BigDecimal("420.00")));

        ChangeAvailabilityResponse response = pricingService.compareForProposedDates(booking, newCheckIn, newCheckOut);

        // proposed total = hotelTotal(550) + carTotal(420) + taxesAndFees(200, non-demo dates) = 1170
        assertEquals(new BigDecimal("1170.00"), response.proposed().total());
        // priceDifference = proposedTotal(1170) - current.total()(booking.getTotalAmount() = 1500)
        assertEquals(new BigDecimal("-330.00"), response.priceDifference());
        assertEquals(new BigDecimal("1500.00"), response.current().total());
    }

    // 10b. taxesAndFeesFor demo dates vs. any other dates
    @Test
    void taxesAndFeesFor_demoProposedDates_returns220_otherDatesReturn200() {
        assertEquals(new BigDecimal("220.00"),
                pricingService.taxesAndFeesFor(LocalDate.of(2027, 3, 15), LocalDate.of(2027, 3, 20)));
        assertEquals(new BigDecimal("200.00"),
                pricingService.taxesAndFeesFor(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5)));
        assertEquals(new BigDecimal("200.00"),
                pricingService.taxesAndFeesFor(LocalDate.of(2027, 3, 15), LocalDate.of(2027, 3, 21)));
    }
}
