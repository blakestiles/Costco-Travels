package com.smartrebook.hotel.repository;

import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.hotel.domain.HotelReservation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotelReservationStore {

    private final Map<String, HotelReservation> reservations = new ConcurrentHashMap<>();

    public HotelReservation save(HotelReservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
        return reservation;
    }

    public Optional<HotelReservation> findById(String id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Optional<HotelReservation> cancel(String id) {
        HotelReservation reservation = reservations.get(id);
        if (reservation == null) {
            return Optional.empty();
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        return Optional.of(reservation);
    }

    public void clear() {
        reservations.clear();
    }
}
