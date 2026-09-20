package com.smartrebook.booking.domain;

import com.smartrebook.contracts.ReservationStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "supplier_reservations")
public class SupplierReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequest changeRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false, length = 20)
    private SupplierType supplierType;

    @Column(name = "supplier_name", nullable = false, length = 50)
    private String supplierName;

    @Column(name = "supplier_confirmation", length = 60)
    private String supplierConfirmation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    protected SupplierReservation() {
    }

    public SupplierReservation(SupplierType supplierType, String supplierName,
                                String supplierConfirmation, ReservationStatus status, BigDecimal amount) {
        this.supplierType = supplierType;
        this.supplierName = supplierName;
        this.supplierConfirmation = supplierConfirmation;
        this.status = status;
        this.amount = amount;
        this.lastCheckedAt = Instant.now();
    }

    void setChangeRequest(ChangeRequest changeRequest) {
        this.changeRequest = changeRequest;
    }

    public Long getId() {
        return id;
    }

    public ChangeRequest getChangeRequest() {
        return changeRequest;
    }

    public SupplierType getSupplierType() {
        return supplierType;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getSupplierConfirmation() {
        return supplierConfirmation;
    }

    public void setSupplierConfirmation(String supplierConfirmation) {
        this.supplierConfirmation = supplierConfirmation;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
        this.lastCheckedAt = Instant.now();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }
}
