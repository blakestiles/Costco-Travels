package com.smartrebook.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "change_requests")
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "correlation_id", nullable = false, length = 40)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "requested_check_in", nullable = false)
    private LocalDate requestedCheckIn;

    @Column(name = "requested_check_out", nullable = false)
    private LocalDate requestedCheckOut;

    @Column(name = "old_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal oldTotal;

    @Column(name = "new_total", precision = 10, scale = 2)
    private BigDecimal newTotal;

    @Column(name = "price_difference", precision = 10, scale = 2)
    private BigDecimal priceDifference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NOT_REQUIRED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Fetched explicitly via @EntityGraph on the ops transaction-detail query to avoid N+1
    // when rendering the timeline for a single change request.
    @OneToMany(mappedBy = "changeRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SupplierReservation> supplierReservations = new ArrayList<>();

    protected ChangeRequest() {
    }

    public ChangeRequest(Booking booking, String correlationId, String idempotencyKey,
                          LocalDate requestedCheckIn, LocalDate requestedCheckOut,
                          BigDecimal oldTotal, ChangeRequestStatus status) {
        this.booking = booking;
        this.correlationId = correlationId;
        this.idempotencyKey = idempotencyKey;
        this.requestedCheckIn = requestedCheckIn;
        this.requestedCheckOut = requestedCheckOut;
        this.oldTotal = oldTotal;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addSupplierReservation(SupplierReservation reservation) {
        supplierReservations.add(reservation);
        reservation.setChangeRequest(this);
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDate getRequestedCheckIn() {
        return requestedCheckIn;
    }

    public LocalDate getRequestedCheckOut() {
        return requestedCheckOut;
    }

    public BigDecimal getOldTotal() {
        return oldTotal;
    }

    public BigDecimal getNewTotal() {
        return newTotal;
    }

    public void setNewTotal(BigDecimal newTotal) {
        this.newTotal = newTotal;
    }

    public BigDecimal getPriceDifference() {
        return priceDifference;
    }

    public void setPriceDifference(BigDecimal priceDifference) {
        this.priceDifference = priceDifference;
    }

    public ChangeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ChangeRequestStatus status) {
        this.status = status;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<SupplierReservation> getSupplierReservations() {
        return supplierReservations;
    }
}
