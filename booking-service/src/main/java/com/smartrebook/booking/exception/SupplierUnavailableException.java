package com.smartrebook.booking.exception;

/** A supplier gave a definitive, immediate failure response (e.g. 503) - not a timeout. */
public class SupplierUnavailableException extends RuntimeException {

    private final String supplier;

    public SupplierUnavailableException(String supplier, String message, Throwable cause) {
        super(message, cause);
        this.supplier = supplier;
    }

    public String getSupplier() {
        return supplier;
    }
}
