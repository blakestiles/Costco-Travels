package com.smartrebook.booking.exception;

public class ChangeInProgressException extends RuntimeException {
    public ChangeInProgressException(String message) {
        super(message);
    }
}
