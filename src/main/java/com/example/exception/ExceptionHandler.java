package com.example.exception;

/**
 * Strategy interface for application-wide exception handling.
 * <p>
 * The default implementation ({@link GlobalExceptionHandler}) shows a JavaFX
 * alert dialog. Tests can inject a different implementation via
 * {@link GlobalExceptionHandler#setInstance(ExceptionHandler)} to capture or
 * assert on exceptions without opening any UI.
 */
public interface ExceptionHandler {

    /**
     * Handles the given throwable — e.g. by logging it and/or notifying the user.
     *
     * @param e the exception to handle
     */
    void handle(Throwable e);
}
