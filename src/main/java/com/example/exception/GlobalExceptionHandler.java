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
 * <p>
 * The active instance can be replaced via {@link #setInstance(ExceptionHandler)},
 * which makes it easy to inject a mock or a recording handler in tests without
 * touching any JavaFX UI.
 */
public final class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Default JavaFX-based handler used in production. */
    private static final ExceptionHandler DEFAULT_HANDLER = e -> {
        log.error("Unhandled exception caught by GlobalExceptionHandler", e);

        Alert.AlertType type;
        String title;
        String message;

        if (e instanceof ValidationException || e instanceof MemberNotFoundException) {
            type    = Alert.AlertType.WARNING;
            title   = "Validation Error";
            message = e.getMessage();
        } else {
            type    = Alert.AlertType.ERROR;
            title   = "Application Error";
            message = "An unexpected error occurred:\n" + e.getMessage();
        }

        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    };

    /** The singleton used by all static {@link #handle(Throwable)} call sites. */
    private static ExceptionHandler instance = DEFAULT_HANDLER;

    /** Utility class — not instantiable. */
    private GlobalExceptionHandler() {}

    // ── Instance management ───────────────────────────────────────────────────

    /**
     * Returns the currently active handler instance.
     *
     * @return current {@link ExceptionHandler}
     */
    public static ExceptionHandler getInstance() {
        return instance;
    }

    /**
     * Replaces the active handler.
     * <p>
     * Call this in tests to inject a mock:
     * <pre>{@code
     *   ExceptionHandler mock = Mockito.mock(ExceptionHandler.class);
     *   GlobalExceptionHandler.setInstance(mock);
     * }</pre>
     * Remember to restore the original after the test (or use
     * {@link #resetInstance()}).
     *
     * @param handler the new handler; must not be {@code null}
     */
    public static void setInstance(ExceptionHandler handler) {
        if (handler == null) throw new IllegalArgumentException("handler must not be null");
        instance = handler;
    }

    /**
     * Restores the default {@link GlobalExceptionHandler} instance.
     * Useful in {@code @AfterEach} test teardowns.
     */
    public static void resetInstance() {
        instance = DEFAULT_HANDLER;
    }

    // ── Static convenience forwarder ──────────────────────────────────────────

    /**
     * Forwards to the active handler instance.
     * Existing call sites ({@code GlobalExceptionHandler.handle(e)}) continue to
     * compile without any changes.
     *
     * @param e the exception to handle
     */
    public static void handle(Throwable e) {
        instance.handle(e);
    }
}
