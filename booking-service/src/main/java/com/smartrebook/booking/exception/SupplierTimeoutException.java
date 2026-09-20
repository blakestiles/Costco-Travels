package com.smartrebook.booking.exception;

/**
 * The client gave up waiting for a supplier response. This does NOT mean the supplier failed -
 * it may have succeeded after our timeout elapsed. Never treated as a confirmed failure; always
 * routed to reconciliation for reservation-creation calls. See README: Ambiguous Supplier Outcome.
 */
public class SupplierTimeoutException extends RuntimeException {

    private final String supplier;

    public SupplierTimeoutException(String supplier, String message, Throwable cause) {
        super(message, cause);
        this.supplier = supplier;
    }

    public String getSupplier() {
        return supplier;
    }
}
