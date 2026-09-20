package com.smartrebook.booking.client;

import com.smartrebook.booking.config.FeignClientConfig;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelReservationRequest;
import com.smartrebook.contracts.hotel.HotelReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "hotel-supplier-service",
        url = "${smartrebook.suppliers.hotel-base-url}",
        configuration = FeignClientConfig.class
)
public interface HotelSupplierClient {

    // Dates are passed as pre-formatted ISO-8601 strings (LocalDate.toString()) rather than
    // LocalDate directly: Feign's default query-param expander otherwise formats LocalDate with
    // a locale-based short style (e.g. "3/15/27") instead of ISO-8601.
    @GetMapping("/api/hotels/availability")
    HotelAvailabilityResponse checkAvailability(
            @RequestParam("destination") String destination,
            @RequestParam("checkIn") String checkIn,
            @RequestParam("checkOut") String checkOut,
            @RequestParam("roomType") String roomType
    );

    @PostMapping("/api/hotels/reservations")
    HotelReservationResponse createReservation(@RequestBody HotelReservationRequest request);

    @GetMapping("/api/hotels/reservations/{id}")
    HotelReservationResponse getReservation(@PathVariable("id") String id);

    @DeleteMapping("/api/hotels/reservations/{id}")
    HotelReservationResponse cancelReservation(@PathVariable("id") String id);

    @PostMapping("/api/hotels/demo/reset")
    void resetDemoState();
}
