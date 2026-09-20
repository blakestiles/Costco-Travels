package com.smartrebook.booking.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, length = 40)
    private String correlationId;

    @Column(name = "external_reference", nullable = false, length = 60)
    private String externalReference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Incident() {
    }

    public Incident(String correlationId, String externalReference, String title, String description,
                     IncidentStatus status) {
        this.correlationId = correlationId;
        this.externalReference = externalReference;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
