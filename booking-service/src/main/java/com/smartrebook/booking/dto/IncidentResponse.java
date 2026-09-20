package com.smartrebook.booking.dto;

public record IncidentResponse(
        String externalReference,
        String title,
        String status
) {
}
