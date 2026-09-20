package com.smartrebook.booking.service;

import com.smartrebook.booking.domain.Booking;
import com.smartrebook.booking.dto.BookingItemDto;
import com.smartrebook.booking.dto.BookingResponse;
import com.smartrebook.booking.dto.ChangeAvailabilityRequest;
import com.smartrebook.booking.dto.ChangeAvailabilityResponse;
import com.smartrebook.booking.exception.BookingNotFoundException;
import com.smartrebook.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingQueryService {

    private final BookingRepository bookingRepository;
    private final TripPricingService pricingService;

    public BookingQueryService(BookingRepository bookingRepository, TripPricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String confirmationNumber) {
        Booking booking = bookingRepository.findWithItemsByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new BookingNotFoundException("No booking found for confirmation " + confirmationNumber));

        return new BookingResponse(
                booking.getConfirmationNumber(), booking.getDestination(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getStatus().name(), booking.getTotalAmount(), booking.getCurrency(),
                booking.getMember().getFirstName() + " " + booking.getMember().getLastName(),
                booking.getMember().getMembershipType(),
                booking.getItems().stream()
                        .map(i -> new BookingItemDto(i.getItemType(), i.getSupplier(), i.getDescription(),
                                i.getAmount(), i.getSupplierConfirmation(), i.getStatus()))
                        .toList()
        );
    }

    /** Read-only: never mutates supplier or local state. See README - Change Page. */
    @Transactional(readOnly = true)
    public ChangeAvailabilityResponse checkAvailability(String confirmationNumber, ChangeAvailabilityRequest request) {
        Booking booking = bookingRepository.findWithItemsByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new BookingNotFoundException("No booking found for confirmation " + confirmationNumber));
        return pricingService.compareForProposedDates(booking, request.newCheckIn(), request.newCheckOut());
    }
}
