package com.smartrebook.booking.domain;

/**
 * Lifecycle of one reservation-change orchestration. This is application-level orchestration
 * with compensating actions inspired by the Saga pattern - not a full Saga implementation.
 */
public enum ChangeRequestStatus {
    REQUESTED,
    VALIDATING,
    CHECKING_AVAILABILITY,
    REPLACEMENT_PENDING,
    HOTEL_RESERVED,
    CAR_RESERVED,
    REPLACEMENT_CONFIRMED,
    CANCELLATION_PENDING,
    COMPLETED,
    COMPENSATION_PENDING,
    FAILED
}
