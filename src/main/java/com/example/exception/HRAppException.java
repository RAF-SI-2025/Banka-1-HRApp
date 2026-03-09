package com.example.exception;

/**
 * Base runtime exception for all HR App domain errors.
 * Use more specific subclasses where possible.
 */
public class HRAppException extends RuntimeException {

    /**
     * Constructs an exception with a descriptive message.
     *
     * @param message human-readable description of the error
     */
    public HRAppException(String message) {
        super(message);
    }

    /**
     * Constructs an exception wrapping an underlying cause.
     *
     * @param message human-readable description of the error
     * @param cause   the underlying exception
     */
    public HRAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
