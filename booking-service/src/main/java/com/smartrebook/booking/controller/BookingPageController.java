package com.smartrebook.booking.controller;

import com.smartrebook.booking.config.DemoMemberContext;
import com.smartrebook.booking.domain.Booking;
import com.smartrebook.booking.dto.BookingSummaryDto;
import com.smartrebook.booking.repository.BookingRepository;
import com.smartrebook.booking.service.BookingQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@org.springframework.web.bind.annotation.RequestMapping("/account/bookings")
public class BookingPageController {

    private final BookingRepository bookingRepository;
    private final BookingQueryService bookingQueryService;
    private final DemoMemberContext memberContext;

    public BookingPageController(BookingRepository bookingRepository, BookingQueryService bookingQueryService,
                                  DemoMemberContext memberContext) {
        this.bookingRepository = bookingRepository;
        this.bookingQueryService = bookingQueryService;
        this.memberContext = memberContext;
    }

    @GetMapping
    public String listBookings(Model model) {
        var bookings = bookingRepository.findByMember_MemberReferenceOrderByCreatedAtDesc(memberContext.currentMemberReference())
                .stream()
                .map(this::toSummary)
                .toList();
        model.addAttribute("currentBookings", bookings.stream().filter(b -> !"CANCELLED".equals(b.status())).toList());
        model.addAttribute("pastBookings", bookings.stream().filter(b -> "CANCELLED".equals(b.status())).toList());
        return "bookings";
    }

    @GetMapping("/{confirmation}")
    public String bookingDetail(@PathVariable String confirmation, Model model) {
        model.addAttribute("booking", bookingQueryService.getBooking(confirmation));
        return "booking-details";
    }

    @GetMapping("/{confirmation}/change")
    public String changeBooking(@PathVariable String confirmation, Model model) {
        model.addAttribute("booking", bookingQueryService.getBooking(confirmation));
        return "change-booking";
    }

    private BookingSummaryDto toSummary(Booking b) {
        return new BookingSummaryDto(b.getConfirmationNumber(), b.getDestination(), b.getCheckInDate(),
                b.getCheckOutDate(), b.getStatus().name(), b.getTotalAmount());
    }
}
