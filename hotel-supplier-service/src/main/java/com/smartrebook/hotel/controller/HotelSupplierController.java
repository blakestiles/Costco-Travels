package com.smartrebook.hotel.controller;

import com.smartrebook.contracts.hotel.HotelAvailabilityRequest;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelReservationRequest;
import com.smartrebook.contracts.hotel.HotelReservationResponse;
import com.smartrebook.hotel.repository.HotelReservationStore;
import com.smartrebook.hotel.service.HotelAvailabilityService;
import com.smartrebook.hotel.service.HotelReservationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hotels")
public class HotelSupplierController {

    private final HotelAvailabilityService availabilityService;
    private final HotelReservationService reservationService;
    private final HotelReservationStore reservationStore;

    public HotelSupplierController(HotelAvailabilityService availabilityService,
                                    HotelReservationService reservationService,
                                    HotelReservationStore reservationStore) {
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;
        this.reservationStore = reservationStore;
    }

    @GetMapping("/availability")
    public ResponseEntity<HotelAvailabilityResponse> checkAvailability(
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam String roomType) {
        HotelAvailabilityRequest request = new HotelAvailabilityRequest(destination, checkIn, checkOut, roomType);
        return ResponseEntity.ok(availabilityService.checkAvailability(request));
    }

    @PostMapping("/reservations")
    public ResponseEntity<HotelReservationResponse> createReservation(
            @RequestBody @Valid HotelReservationRequest request,
            @RequestHeader(value = "X-Demo-Scenario", required = false) String demoScenario) {
        HotelReservationResponse response = reservationService.createReservation(request, demoScenario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<HotelReservationResponse> getReservation(@PathVariable String id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<HotelReservationResponse> cancelReservation(@PathVariable String id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @PostMapping("/demo/reset")
    public ResponseEntity<Void> resetDemo() {
        reservationStore.clear();
        return ResponseEntity.noContent().build();
    }
}
