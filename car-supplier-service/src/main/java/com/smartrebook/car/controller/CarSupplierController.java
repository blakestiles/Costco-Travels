package com.smartrebook.car.controller;

import com.smartrebook.car.service.CarAvailabilityService;
import com.smartrebook.car.service.CarReservationService;
import com.smartrebook.contracts.car.CarAvailabilityRequest;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.car.CarReservationRequest;
import com.smartrebook.contracts.car.CarReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cars")
public class CarSupplierController {

    private final CarAvailabilityService availabilityService;
    private final CarReservationService reservationService;

    public CarSupplierController(CarAvailabilityService availabilityService, CarReservationService reservationService) {
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;
    }

    @GetMapping("/availability")
    public ResponseEntity<CarAvailabilityResponse> checkAvailability(
            @RequestParam LocalDate pickupDate,
            @RequestParam LocalDate returnDate,
            @RequestParam String carClass) {
        CarAvailabilityRequest request = new CarAvailabilityRequest(pickupDate, returnDate, carClass);
        return ResponseEntity.ok(availabilityService.checkAvailability(request));
    }

    @PostMapping("/reservations")
    public ResponseEntity<CarReservationResponse> createReservation(
            @RequestBody @Valid CarReservationRequest request,
            @RequestHeader(value = "X-Demo-Scenario", required = false) String demoScenario) {
        CarReservationResponse response = reservationService.createReservation(request, demoScenario);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<CarReservationResponse> getReservation(@PathVariable String id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping("/reservations/by-client-reference/{reference}")
    public ResponseEntity<CarReservationResponse> getByClientReference(@PathVariable String reference) {
        return ResponseEntity.ok(reservationService.getByClientReference(reference));
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<CarReservationResponse> cancelReservation(@PathVariable String id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }
}
