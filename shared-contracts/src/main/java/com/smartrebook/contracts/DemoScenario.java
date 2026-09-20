package com.smartrebook.contracts;

/**
 * Demo-only supplier behavior toggle, propagated from booking-service to the
 * supplier services via the X-Demo-Scenario header. Not part of the public API surface.
 */
public enum DemoScenario {
    NORMAL,
    HOTEL_FAILURE,
    CAR_TIMEOUT,
    CAR_AMBIGUOUS
}
