package com.smartrebook.booking.dto;

import java.time.Instant;

public record BookingEventDto(
        String eventType,
        String supplier,
        String status,
        String message,
        Long latencyMs,
        Instant createdAt
) {
}
