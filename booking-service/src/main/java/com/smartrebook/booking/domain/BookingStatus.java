package com.smartrebook.booking.domain;

/** Status of a member's booking. Deliberately not a boolean - see README: Booking Orchestration. */
public enum BookingStatus {
    CONFIRMED,
    CHANGE_PENDING,
    CANCELLATION_PENDING,
    CANCELLED,
    FAILED
}
