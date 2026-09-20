package com.smartrebook.booking.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One row per meaningful timeline moment. The Operations transaction timeline is rendered
 * directly from these rows - never hardcoded. Rows with a non-null supplier + latencyMs and
 * eventType = "SUPPLIER_CALL" also feed sp_supplier_reliability_report (see V3 migration).
 */
@Entity
@Table(name = "booking_events")
public class BookingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "change_request_id")
    private Long changeRequestId;

    @Column(name = "correlation_id", nullable = false, length = 40)
    private String correlationId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(length = 50)
    private String supplier;

    @Column(length = 30)
    private String status;

    @Column(length = 500)
    private String message;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookingEvent() {
    }

    private BookingEvent(Long bookingId, Long changeRequestId, String correlationId, String eventType,
                          String supplier, String status, String message, Long latencyMs) {
        this.bookingId = bookingId;
        this.changeRequestId = changeRequestId;
        this.correlationId = correlationId;
        this.eventType = eventType;
        this.supplier = supplier;
        this.status = status;
        this.message = message;
        this.latencyMs = latencyMs;
    }

    public static BookingEvent lifecycle(Long bookingId, Long changeRequestId, String correlationId,
                                          String eventType, String message) {
        return new BookingEvent(bookingId, changeRequestId, correlationId, eventType, null, null, message, null);
    }

    public static BookingEvent supplierCall(Long bookingId, Long changeRequestId, String correlationId,
                                             String supplier, String status, String message, long latencyMs) {
        return new BookingEvent(bookingId, changeRequestId, correlationId, "SUPPLIER_CALL",
                supplier, status, message, latencyMs);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getChangeRequestId() {
        return changeRequestId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
