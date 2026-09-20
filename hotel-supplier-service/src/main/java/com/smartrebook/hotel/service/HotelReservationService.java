package com.smartrebook.hotel.service;

import com.smartrebook.contracts.DemoScenario;
import com.smartrebook.contracts.ReservationStatus;
import com.smartrebook.contracts.hotel.HotelReservationRequest;
import com.smartrebook.contracts.hotel.HotelReservationResponse;
import com.smartrebook.hotel.domain.HotelReservation;
import com.smartrebook.hotel.exception.HotelSupplierUnavailableException;
import com.smartrebook.hotel.exception.ReservationNotFoundException;
import com.smartrebook.hotel.repository.HotelReservationStore;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class HotelReservationService {

    private final HotelReservationStore store;

    public HotelReservationService(HotelReservationStore store) {
        this.store = store;
    }

    public HotelReservationResponse createReservation(HotelReservationRequest request, String demoScenarioHeader) {
        DemoScenario scenario = parseScenario(demoScenarioHeader);

        if (scenario == DemoScenario.HOTEL_FAILURE) {
            throw new HotelSupplierUnavailableException("Hotel supplier is currently unavailable");
        }

        String reservationId = UUID.randomUUID().toString();
        String confirmationNumber = "HTL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        HotelReservation reservation = new HotelReservation(
                reservationId,
                confirmationNumber,
                request.clientReference(),
                request.destination(),
                request.checkIn(),
                request.checkOut(),
                request.hotel(),
                request.room(),
                request.nightlyRate(),
                request.taxes(),
                request.total(),
                ReservationStatus.CONFIRMED
        );
        store.save(reservation);

        return toResponse(reservation);
    }

    public HotelReservationResponse findById(String id) {
        HotelReservation reservation = store.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
        return toResponse(reservation);
    }

    public HotelReservationResponse cancel(String id) {
        HotelReservation reservation = store.cancel(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
        return toResponse(reservation);
    }

    private HotelReservationResponse toResponse(HotelReservation reservation) {
        return new HotelReservationResponse(
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
}
