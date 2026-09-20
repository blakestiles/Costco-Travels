package com.smartrebook.car.repository;

import com.smartrebook.car.domain.CarReservation;
import com.smartrebook.contracts.ReservationStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CarReservationStore {

    private final Map<String, CarReservation> byId = new ConcurrentHashMap<>();
    private final Map<String, String> idByClientReference = new ConcurrentHashMap<>();

    public CarReservation save(CarReservation reservation) {
        byId.put(reservation.getReservationId(), reservation);
        idByClientReference.put(reservation.getClientReference(), reservation.getReservationId());
        return reservation;
    }

    public Optional<CarReservation> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<CarReservation> findByClientReference(String clientReference) {
        String id = idByClientReference.get(clientReference);
        if (id == null) {
            return Optional.empty();
        }
        return findById(id);
    }

    public Optional<CarReservation> cancel(String id) {
        CarReservation reservation = byId.get(id);
        if (reservation == null) {
            return Optional.empty();
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return Optional.of(reservation);
    }

    public void clear() {
        byId.clear();
        idByClientReference.clear();
    }
}
