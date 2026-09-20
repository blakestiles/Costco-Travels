package com.smartrebook.booking.exception;

/** Same Idempotency-Key reused with a materially different request body. */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
