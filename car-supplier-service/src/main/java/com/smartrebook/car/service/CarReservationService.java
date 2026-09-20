package com.smartrebook.car.service;

import com.smartrebook.car.domain.CarReservation;
import com.smartrebook.car.exception.CarSupplierTimeoutException;
import com.smartrebook.car.exception.ReservationNotFoundException;
import com.smartrebook.car.repository.CarReservationStore;
import com.smartrebook.contracts.DemoScenario;
import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.contracts.car.CarReservationRequest;
import com.smartrebook.contracts.car.CarReservationResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CarReservationService {

    private static final long TIMEOUT_SCENARIO_DELAY_MS = 8000L;

    private final CarReservationStore store;

    public CarReservationService(CarReservationStore store) {
        this.store = store;
    }

    public CarReservationResponse createReservation(CarReservationRequest request, String demoScenarioHeader) {
        DemoScenario scenario = parseScenario(demoScenarioHeader);

        if (scenario == DemoScenario.CAR_TIMEOUT) {
            sleep(TIMEOUT_SCENARIO_DELAY_MS);
            throw new CarSupplierTimeoutException("Car supplier timed out processing reservation request");
        }

        if (scenario == DemoScenario.CAR_AMBIGUOUS) {
            CarReservation reservation = persistReservation(request);
            sleep(TIMEOUT_SCENARIO_DELAY_MS);
            return toResponse(reservation);
        }

        CarReservation reservation = persistReservation(request);
        return toResponse(reservation);
    }

    public CarReservationResponse getById(String id) {
        return store.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException("No car reservation found with id " + id));
    }

    public CarReservationResponse getByClientReference(String clientReference) {
        return store.findByClientReference(clientReference)
                .map(this::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "No car reservation found with clientReference " + clientReference));
    }

    public CarReservationResponse cancel(String id) {
        return store.cancel(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ReservationNotFoundException("No car reservation found with id " + id));
    }

    private CarReservation persistReservation(CarReservationRequest request) {
        String reservationId = UUID.randomUUID().toString();
        String confirmationNumber = "CAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CarReservation reservation = new CarReservation(
                reservationId,
                request.clientReference(),
                request.pickupDate(),
                request.returnDate(),
                request.carClass(),
                request.dailyRate(),
                request.taxes(),
                request.total(),
                confirmationNumber,
                ReservationStatus.CONFIRMED
        );
        return store.save(reservation);
    }

    private CarReservationResponse toResponse(CarReservation reservation) {
        return new CarReservationResponse(
                reservation.getReservationId(),
                reservation.getClientReference(),
                reservation.getStatus(),
                reservation.getConfirmationNumber(),
                reservation.getTotal()
        );
    }

    private DemoScenario parseScenario(String header) {
        if (header == null || header.isBlank()) {
            return DemoScenario.NORMAL;
        }
        try {
            return DemoScenario.valueOf(header.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DemoScenario.NORMAL;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
