package com.example.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link GlobalExceptionHandler} instance-swap mechanism.
 * <p>
 * These tests verify that:
 * <ul>
 *   <li>the static {@code handle()} forwarder delegates to the active instance</li>
 *   <li>a custom {@link ExceptionHandler} can be injected and receives calls</li>
 *   <li>{@code resetInstance()} restores the default handler</li>
 *   <li>{@code setInstance(null)} is rejected with an {@link IllegalArgumentException}</li>
 * </ul>
 * No JavaFX toolkit is started — the default handler is never called directly.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ExceptionHandler mockHandler;

    /** Replace the real handler with a mock before every test. */
    @BeforeEach
    void setUp() {
        GlobalExceptionHandler.setInstance(mockHandler);
    }

    /** Always restore the default so other tests are not affected. */
    @AfterEach
    void tearDown() {
        GlobalExceptionHandler.resetInstance();
    }

    // ── forwarding ────────────────────────────────────────────────────────────

    @Test
    void handle_delegatesToActiveInstance() {
        RuntimeException ex = new RuntimeException("boom");

        GlobalExceptionHandler.handle(ex);

        verify(mockHandler).handle(ex);
    }

    @Test
    void handle_withValidationException_delegatesToActiveInstance() {
        ValidationException ex = new ValidationException("name is blank");

        GlobalExceptionHandler.handle(ex);

        verify(mockHandler).handle(ex);
    }

    @Test
    void handle_withMemberNotFoundException_delegatesToActiveInstance() {
        MemberNotFoundException ex = new MemberNotFoundException(99L);

        GlobalExceptionHandler.handle(ex);

        verify(mockHandler).handle(ex);
    }

    @Test
    void handle_forwardsExactThrowableInstance() {
        Throwable ex = new IllegalStateException("state error");

        GlobalExceptionHandler.handle(ex);

        verify(mockHandler, times(1)).handle(same(ex));
    }

    @Test
    void handle_calledMultipleTimes_allForwardedToInstance() {
        RuntimeException ex1 = new RuntimeException("first");
        RuntimeException ex2 = new RuntimeException("second");

        GlobalExceptionHandler.handle(ex1);
        GlobalExceptionHandler.handle(ex2);

        verify(mockHandler).handle(ex1);
        verify(mockHandler).handle(ex2);
        verifyNoMoreInteractions(mockHandler);
    }

    // ── instance management ───────────────────────────────────────────────────

    @Test
    void getInstance_returnsCurrentlySetInstance() {
        assertSame(mockHandler, GlobalExceptionHandler.getInstance());
    }

    @Test
    void setInstance_replacesActiveHandler() {
        ExceptionHandler anotherMock = mock(ExceptionHandler.class);
        GlobalExceptionHandler.setInstance(anotherMock);

        RuntimeException ex = new RuntimeException("test");
        GlobalExceptionHandler.handle(ex);

        verify(anotherMock).handle(ex);
        verifyNoInteractions(mockHandler);
    }

    @Test
    void setInstance_withNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> GlobalExceptionHandler.setInstance(null));
    }

    @Test
    void resetInstance_restoresDefaultHandler() {
        GlobalExceptionHandler.resetInstance();

        assertNotSame(mockHandler, GlobalExceptionHandler.getInstance());
    }
}
