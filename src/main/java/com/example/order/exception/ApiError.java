package com.example.order.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard API error response payload.
 *
 * <p>Provides comprehensive error details including HTTP status, message,
 * field-level validation errors, and timestamp for audit trails.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /**
     * HTTP status code (e.g., 400, 404, 500).
     */
    private int status;

    /**
     * HTTP reason phrase (e.g., "Bad Request").
     */
    private String error;

    /**
     * Human-readable primary error message.
     */
    private String message;

    /**
     * Request path or endpoint where the error occurred.
     */
    private String path;

    /**
     * ISO 8601 timestamp when the error occurred.
     */
    private LocalDateTime timestamp;

    /**
     * List of field-level validation errors (may be null).
     * Each item describes one field validation failure with actionable guidance.
     */
    private List<FieldError> fieldErrors;

    /**
     * Generic error details or exception stack trace for debugging (may be null).
     */
    private String details;

    /**
     * Represents a single field-level validation error.
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class FieldError {

        /**
         * Name of the field that failed validation.
         */
        private String field;

        /**
         * The value that was rejected.
         */
        private String rejectedValue;

        /**
         * Specific validation failure message.
         */
        private String message;
    }
}