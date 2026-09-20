package com.smartrebook.booking.domain;

/**
 * Tracked independently of ChangeRequestStatus.status: a change request can be terminally FAILED
 * while its reconciliation is still REQUIRED. Timeout does not mean confirmed failure - see
 * README: Ambiguous Supplier Outcome.
 */
public enum ReconciliationStatus {
    NOT_REQUIRED,
    REQUIRED,
    IN_PROGRESS,
    RESOLVED
}
