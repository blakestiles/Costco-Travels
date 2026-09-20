package com.smartrebook.booking.client;

import com.smartrebook.booking.exception.SupplierTimeoutException;
import com.smartrebook.booking.exception.SupplierUnavailableException;
import com.smartrebook.contracts.car.CarAvailabilityResponse;
import com.smartrebook.contracts.car.CarReservationRequest;
import com.smartrebook.contracts.car.CarReservationResponse;
import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class CarSupplierGateway {

    private final CarSupplierClient client;

    public CarSupplierGateway(CarSupplierClient client) {
        this.client = client;
    }

    @Retry(name = "carAvailability")
    @CircuitBreaker(name = "carSupplier")
    public CarAvailabilityResponse checkAvailability(LocalDate pickupDate, LocalDate returnDate, String carClass) {
        try {
            return client.checkAvailability(pickupDate.toString(), returnDate.toString(), carClass);
        } catch (RetryableException e) {
            throw new SupplierTimeoutException("CAR", "Car availability check timed out", e);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("CAR", "Car supplier is unavailable", e);
        }
    }

    /**
     * No retry, no circuit breaker fallback here: a timeout on this call is genuinely ambiguous
     * (the supplier may have created the reservation and just responded late) and must surface
     * as SupplierTimeoutException so the orchestrator routes it to reconciliation rather than
     * assuming failure. See README: Ambiguous Supplier Outcome.
     */
    public CarReservationResponse createReservation(CarReservationRequest request) {
        try {
            return client.createReservation(request);
        } catch (RetryableException e) {
            throw new SupplierTimeoutException("CAR", "Car reservation request timed out", e);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("CAR", "Car supplier is unavailable", e);
        }
    }

    public void cancelReservation(String reservationId) {
        try {
            client.cancelReservation(reservationId);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("CAR", "Car supplier is unavailable", e);
        }
    }

    /** Used only by reconciliation: a 404 here means "no orphan reservation" - never throw for that. */
    public Optional<CarReservationResponse> findByClientReference(String clientReference) {
        try {
            return Optional.ofNullable(client.getReservationByClientReference(clientReference));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        } catch (RetryableException e) {
            throw new SupplierTimeoutException("CAR", "Reconciliation lookup timed out", e);
        } catch (FeignException e) {
            throw new SupplierUnavailableException("CAR", "Car supplier is unavailable", e);
        }
    }
}
