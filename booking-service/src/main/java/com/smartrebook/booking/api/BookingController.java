package com.smartrebook.booking.api;

import com.smartrebook.booking.dto.*;
import com.smartrebook.booking.orchestration.RebookOrchestrator;
import com.smartrebook.booking.service.BookingQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingQueryService bookingQueryService;
    private final RebookOrchestrator orchestrator;

    public BookingController(BookingQueryService bookingQueryService, RebookOrchestrator orchestrator) {
        this.bookingQueryService = bookingQueryService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/{confirmation}")
    public BookingResponse getBooking(@PathVariable String confirmation) {
        return bookingQueryService.getBooking(confirmation);
    }

    @PostMapping("/{confirmation}/change/availability")
    public ChangeAvailabilityResponse checkAvailability(@PathVariable String confirmation,
                                                          @Valid @RequestBody ChangeAvailabilityRequest request) {
        return bookingQueryService.checkAvailability(confirmation, request);
    }

    @PostMapping("/{confirmation}/change")
    @ResponseStatus(HttpStatus.OK)
    public ChangeRequestResponse submitChange(@PathVariable String confirmation,
                                               @Valid @RequestBody ChangeSubmitRequest request,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return orchestrator.submitChange(confirmation, request, idempotencyKey);
    }
}
