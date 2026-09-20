package com.smartrebook.booking.exception;

public record ApiError(String code, String message, String correlationId) {
}
