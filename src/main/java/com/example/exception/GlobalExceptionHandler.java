package com.example.exception;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralised exception handler for the UI layer.
 * <p>
 * All service/repository exceptions bubble up to the view and are passed here
 * instead of scattering try/catch blocks across every dialog and controller.
 */
public final class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {
        // Utility class — not instantiable.
    }

    /**
     * Handles an exception by logging it and displaying an appropriate alert to the user.
     * <p>
     * Validation and "not found" errors are shown as warnings; all other errors
     * are shown as errors with the full message.
     *
     * @param e the exception to handle
     */
    public static void handle(Throwable e) {
        log.error("Unhandled exception caught by GlobalExceptionHandler", e);

        Alert.AlertType type;
        String title;
        String message;

        if (e instanceof ValidationException || e instanceof MemberNotFoundException) {
            type = Alert.AlertType.WARNING;
            title = "Validation Error";
            message = e.getMessage();
        } else {
            type = Alert.AlertType.ERROR;
            title = "Application Error";
            message = "An unexpected error occurred:\n" + e.getMessage();
        }

        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
