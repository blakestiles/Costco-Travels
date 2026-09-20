package com.smartrebook.car.domain;

import com.smartrebook.contracts.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CarReservation {

    private String reservationId;
    private String clientReference;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private String carClass;
    private BigDecimal dailyRate;
    private BigDecimal taxes;
    private BigDecimal total;
    private String confirmationNumber;
    private ReservationStatus status;

    public CarReservation() {
    }

    public CarReservation(String reservationId, String clientReference, LocalDate pickupDate, LocalDate returnDate,
                           String carClass, BigDecimal dailyRate, BigDecimal taxes, BigDecimal total,
                           String confirmationNumber, ReservationStatus status) {
        this.reservationId = reservationId;
        this.clientReference = clientReference;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.carClass = carClass;
        this.dailyRate = dailyRate;
        this.taxes = taxes;
        this.total = total;
        this.confirmationNumber = confirmationNumber;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getClientReference() {
        return clientReference;
    }

    public void setClientReference(String clientReference) {
        this.clientReference = clientReference;
    }

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getCarClass() {
        return carClass;
    }

    public void setCarClass(String carClass) {
        this.carClass = carClass;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
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

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
