package com.smartrebook.booking.client;

import com.smartrebook.booking.config.FeignClientConfig;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.car.CarReservationRequest;
import com.smartrebook.contracts.car.CarReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "car-supplier-service",
        url = "${smartrebook.suppliers.car-base-url}",
        configuration = FeignClientConfig.class
)
public interface CarSupplierClient {

    // Dates are pre-formatted ISO-8601 strings - see HotelSupplierClient for why.
    @GetMapping("/api/cars/availability")
    CarAvailabilityResponse checkAvailability(
            @RequestParam("pickupDate") String pickupDate,
            @RequestParam("returnDate") String returnDate,
            @RequestParam("carClass") String carClass
    );

    @PostMapping("/api/cars/reservations")
    CarReservationResponse createReservation(@RequestBody CarReservationRequest request);

    @GetMapping("/api/cars/reservations/{id}")
    CarReservationResponse getReservation(@PathVariable("id") String id);

    @GetMapping("/api/cars/reservations/by-client-reference/{reference}")
    CarReservationResponse getReservationByClientReference(@PathVariable("reference") String reference);

    @DeleteMapping("/api/cars/reservations/{id}")
    CarReservationResponse cancelReservation(@PathVariable("id") String id);

    @PostMapping("/api/cars/demo/reset")
    void resetDemoState();
}
