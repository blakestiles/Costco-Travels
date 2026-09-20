package com.smartrebook.booking.exception;

import com.smartrebook.booking.config.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Consistent {code, message, correlationId} error body for every API error. No stack traces to the browser. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BookingNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidBookingStateException ex) {
        return respond(HttpStatus.CONFLICT, "INVALID_BOOKING_STATE", ex.getMessage());
    }

    @ExceptionHandler(ChangeInProgressException.class)
    public ResponseEntity<ApiError> handleChangeInProgress(ChangeInProgressException ex) {
        return respond(HttpStatus.CONFLICT, "CHANGE_IN_PROGRESS", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return respond(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return respond(HttpStatus.CONFLICT, "CHANGE_IN_PROGRESS",
                "Another change is already being processed for this reservation.");
    }

    @ExceptionHandler(SupplierUnavailableException.class)
    public ResponseEntity<ApiError> handleSupplierUnavailable(SupplierUnavailableException ex) {
        return respond(HttpStatus.BAD_GATEWAY, "SUPPLIER_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(SupplierTimeoutException.class)
    public ResponseEntity<ApiError> handleSupplierTimeout(SupplierTimeoutException ex) {
        return respond(HttpStatus.GATEWAY_TIMEOUT, "SUPPLIER_TIMEOUT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Invalid request");
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return ResponseEntity.status(status).body(new ApiError(code, message, correlationId));
    }
}
