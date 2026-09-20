package com.smartrebook.booking.service;

import com.smartrebook.booking.client.CarSupplierGateway;
import com.smartrebook.booking.client.HotelSupplierGateway;
import com.smartrebook.booking.domain.Booking;
import com.smartrebook.booking.domain.BookingItem;
import com.smartrebook.booking.dto.ChangeAvailabilityResponse;
import com.smartrebook.booking.dto.TripSummary;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Taxes & fees are looked up by date range rather than computed from a real tax engine - pricing
 * and tax computation are out of scope for this prototype (see README: What Is Mocked). A real
 * implementation would call a pricing/tax service; this keeps the demo numbers deterministic.
 */
@Service
public class TripPricingService {

    private static final BigDecimal DEFAULT_TAXES_AND_FEES = new BigDecimal("200.00");
    private static final LocalDate DEMO_PROPOSED_CHECK_IN = LocalDate.of(2027, 3, 15);
    private static final LocalDate DEMO_PROPOSED_CHECK_OUT = LocalDate.of(2027, 3, 20);
    private static final BigDecimal DEMO_PROPOSED_TAXES_AND_FEES = new BigDecimal("220.00");

    private final HotelSupplierGateway hotelGateway;
    private final CarSupplierGateway carGateway;

    public TripPricingService(HotelSupplierGateway hotelGateway, CarSupplierGateway carGateway) {
        this.hotelGateway = hotelGateway;
        this.carGateway = carGateway;
    }

    public TripSummary currentTripSummary(Booking booking) {
        BigDecimal hotelTotal = itemAmount(booking, "HOTEL");
        BigDecimal carTotal = itemAmount(booking, "CAR");
        BigDecimal taxesAndFees = itemAmount(booking, "FEES");
        return new TripSummary(booking.getCheckInDate(), booking.getCheckOutDate(),
                hotelTotal, carTotal, taxesAndFees, booking.getTotalAmount());
    }

    public ChangeAvailabilityResponse compareForProposedDates(Booking booking, LocalDate newCheckIn, LocalDate newCheckOut) {
        TripSummary current = currentTripSummary(booking);

        HotelAvailabilityResponse hotelAvailability = hotelGateway.checkAvailability(
                booking.getDestination(), newCheckIn, newCheckOut, TripConstants.ROOM_TYPE);
        CarAvailabilityResponse carAvailability = carGateway.checkAvailability(
                newCheckIn, newCheckOut, TripConstants.CAR_CLASS);

        BigDecimal taxesAndFees = taxesAndFeesFor(newCheckIn, newCheckOut);
        BigDecimal hotelTotal = hotelAvailability.available() ? hotelAvailability.total() : BigDecimal.ZERO;
        BigDecimal carTotal = carAvailability.available() ? carAvailability.total() : BigDecimal.ZERO;
        BigDecimal proposedTotal = hotelTotal.add(carTotal).add(taxesAndFees);

        TripSummary proposed = new TripSummary(newCheckIn, newCheckOut, hotelTotal, carTotal, taxesAndFees, proposedTotal);
        BigDecimal difference = proposedTotal.subtract(current.total());

        return new ChangeAvailabilityResponse(
                current, proposed, difference,
                hotelAvailability.available(), carAvailability.available(),
                "$200 Digital Costco Shop Card preserved"
        );
    }

    public BigDecimal taxesAndFeesFor(LocalDate checkIn, LocalDate checkOut) {
        if (DEMO_PROPOSED_CHECK_IN.equals(checkIn) && DEMO_PROPOSED_CHECK_OUT.equals(checkOut)) {
            return DEMO_PROPOSED_TAXES_AND_FEES;
        }
        return DEFAULT_TAXES_AND_FEES;
    }

    private BigDecimal itemAmount(Booking booking, String itemType) {
        return booking.getItems().stream()
                .filter(item -> item.getItemType().equals(itemType))
                .map(BookingItem::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
