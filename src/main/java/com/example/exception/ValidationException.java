package com.example.exception;

/**
 * Thrown when user-supplied input fails validation rules
 * (e.g. blank name, grade out of range).
 */
public class ValidationException extends HRAppException {

    /**
     * Constructs a validation exception with a descriptive message shown to the user.
     *
     * @param message description of the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }
}
