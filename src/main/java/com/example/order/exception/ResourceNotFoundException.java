package com.example.order.exception;

/**
 * Exception thrown when a requested resource does not exist.
 *
 * <p>Caught by GlobalExceptionHandler and converted to a 404 JSON response
 * for REST endpoints, or an error page for UI endpoints.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates exception with a message.
     *
     * @param message exception message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates exception with a message and cause.
     *
     * @param message exception message
     * @param cause root cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}