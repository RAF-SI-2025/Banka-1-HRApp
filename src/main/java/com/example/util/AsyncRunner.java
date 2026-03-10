package com.example.util;

import com.example.exception.GlobalExceptionHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility for running database / service calls off the JavaFX Application Thread.
 * <p>
 * All {@code dbCall} lambdas execute on a shared daemon thread pool.
 * All {@code onSuccess} / {@code onFailure} callbacks are guaranteed to run
 * back on the JavaFX Application Thread, so it is safe to touch UI nodes inside them.
 */
public final class AsyncRunner {

    private static final ExecutorService EXECUTOR =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "async-db-thread");
                t.setDaemon(true); // won't block JVM shutdown
                return t;
            });

    /**
     * Runs {@code dbCall} on a background thread, then delivers the result
     * to {@code onSuccess} on the JavaFX Application Thread.
     *
     * @param dbCall    the blocking call (DB / service layer)
     * @param onSuccess callback that receives the result on the UI thread
     * @param <T>       result type
     */
    public static <T> void run(Supplier<T> dbCall, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return dbCall.get();
            }
        };

        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() ->
                    GlobalExceptionHandler.handle(
                            ex instanceof Exception cause ? cause : new RuntimeException(ex)));
        });

        EXECUTOR.submit(task);
    }

    /**
     * Overload for void DB operations (insert / update / delete).
     *
     * @param dbCall    the blocking call
     * @param onSuccess callback run on the UI thread after success
     */
    public static void run(Runnable dbCall, Runnable onSuccess) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                dbCall.run();
                return null;
            }
        };

        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() ->
                    GlobalExceptionHandler.handle(
                            ex instanceof Exception cause ? cause : new RuntimeException(ex)));
        });

        EXECUTOR.submit(task);
    }

    private AsyncRunner() {}
}

