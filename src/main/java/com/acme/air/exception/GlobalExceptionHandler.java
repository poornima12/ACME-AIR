package com.acme.air.exception;

import com.acme.air.dto.ErrorResponse;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        logger.error("Validation failed for request: {}", errors);

        String errorMessage = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.error("HTTP message not readable", ex);
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException invalidEx) {
            String field = invalidEx.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .reduce((first, second) -> second)
                    .orElse("unknown");
            String message = String.format("Invalid format for field '%s': value '%s' is not valid",
                    field, invalidEx.getValue());
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse("INVALID_FORMAT", message));
        }
        // fallback for other parsing issues
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "Malformed JSON or type mismatch in request body"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Illegal argument", ex);
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.error("Resource not found", ex);
        ErrorResponse error = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BookingConflictException ex) {
        logger.error("Booking conflict", ex);
        ErrorResponse error = new ErrorResponse("BOOKING_CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSeatUnavailable(SeatUnavailableException ex) {
        logger.error("Seat unavailable", ex);
        ErrorResponse error = new ErrorResponse("SEAT_UNAVAILABLE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex); // Important: Log the actual exception
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}