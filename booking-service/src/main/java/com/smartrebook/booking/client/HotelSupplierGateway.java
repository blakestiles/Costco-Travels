package com.smartrebook.booking.client;

import com.smartrebook.booking.exception.SupplierTimeoutException;
import com.smartrebook.booking.exception.SupplierUnavailableException;
import com.smartrebook.contracts.hotel.HotelAvailabilityResponse;
import com.smartrebook.contracts.hotel.HotelReservationRequest;
import com.smartrebook.contracts.hotel.HotelReservationResponse;
import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Wraps the raw Feign client with resilience behavior and translates transport-level failures
 * into our own exception types the orchestrator understands. Availability GETs get a bounded
 * retry (safe - read-only); reservation POSTs are never blindly retried (see README: Retries).
 */
@Component
public class HotelSupplierGateway {

    private final HotelSupplierClient client;

    public HotelSupplierGateway(HotelSupplierClient client) {
        this.client = client;
    }

    @Retry(name = "hotelAvailability")
    @CircuitBreaker(name = "hotelSupplier")
    public HotelAvailabilityResponse checkAvailability(String destination, LocalDate checkIn,
                                                         LocalDate checkOut, String roomType) {
        try {
            return client.checkAvailability(destination, checkIn.toString(), checkOut.toString(), roomType);
        } catch (RetryableException e) {
            throw new SupplierTimeoutException("HOTEL", "Hotel availability check timed out", e);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("HOTEL", "Hotel supplier is unavailable", e);
        }
    }

    @CircuitBreaker(name = "hotelSupplier")
    public HotelReservationResponse createReservation(HotelReservationRequest request) {
        try {
            return client.createReservation(request);
        } catch (RetryableException e) {
            throw new SupplierTimeoutException("HOTEL", "Hotel reservation request timed out", e);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("HOTEL", "Hotel supplier is unavailable", e);
        }
    }

    public void cancelReservation(String reservationId) {
        try {
            client.cancelReservation(reservationId);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("HOTEL", "Hotel supplier is unavailable", e);
        }
    }
}
