package com.smartrebook.hotel.domain;

import com.smartrebook.contracts.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HotelReservation {

    private String reservationId;
    private String confirmationNumber;
    private String clientReference;
    private String destination;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String hotel;
    private String room;
    private BigDecimal nightlyRate;
    private BigDecimal taxes;
    private BigDecimal total;
    private ReservationStatus status;

    public HotelReservation() {
    }

    public HotelReservation(String reservationId, String confirmationNumber, String clientReference,
                             String destination, LocalDate checkIn, LocalDate checkOut, String hotel,
                             String room, BigDecimal nightlyRate, BigDecimal taxes, BigDecimal total,
                             ReservationStatus status) {
        this.reservationId = reservationId;
        this.confirmationNumber = confirmationNumber;
        this.clientReference = clientReference;
        this.destination = destination;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.hotel = hotel;
        this.room = room;
        this.nightlyRate = nightlyRate;
        this.taxes = taxes;
        this.total = total;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public String getClientReference() {
        return clientReference;
    }

    public void setClientReference(String clientReference) {
        this.clientReference = clientReference;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public String getHotel() {
        return hotel;
    }

    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(BigDecimal nightlyRate) {
        this.nightlyRate = nightlyRate;
    }

    public BigDecimal getTaxes() {
        return taxes;
    }

    public void setTaxes(BigDecimal taxes) {
        this.taxes = taxes;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
