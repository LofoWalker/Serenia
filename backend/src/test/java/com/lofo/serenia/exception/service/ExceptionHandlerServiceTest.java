package com.lofo.serenia.exception.service;

import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.exception.handler.*;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.inject.Instance;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExceptionHandlerService.
 */
@DisplayName("ExceptionHandlerService")
class ExceptionHandlerServiceTest {

    private static final String TEST_PATH = "/api/test";
    private static final String TEST_TRACE_ID = "trace-123";

    private ExceptionHandlerService service;
    private Instance<ExceptionHandler> handlers;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        handlers = mock(Instance.class);
    }

    private void configureHandlers(ExceptionHandler... handlerArray) {
        List<ExceptionHandler> handlerList = List.of(handlerArray);
        when(handlers.spliterator()).thenReturn(handlerList.spliterator());
        service = new ExceptionHandlerService(handlers);
    }

    @Nested
    @DisplayName("handleException")
    class HandleException {

        @Test
        @DisplayName("should route AuthenticationFailedException to AuthenticationFailedHandler")
        void shouldRouteAuthenticationFailedException() {
            AuthenticationFailedHandler authHandler = new AuthenticationFailedHandler();
            GenericExceptionHandler genericHandler = new GenericExceptionHandler();
            configureHandlers(genericHandler, authHandler);

            AuthenticationFailedException exception = new AuthenticationFailedException("Token invalid");

            ErrorResponse response = service.handleException(exception, TEST_PATH, TEST_TRACE_ID);

            assertThat(response.status()).isEqualTo(401);
            assertThat(response.error()).isEqualTo("AUTHENTICATION_FAILED");
        }

        @Test
        @DisplayName("should route ForbiddenAccessException to ForbiddenAccessHandler")
        void shouldRouteForbiddenAccessException() {
            ForbiddenAccessHandler forbiddenHandler = new ForbiddenAccessHandler();
            GenericExceptionHandler genericHandler = new GenericExceptionHandler();
            configureHandlers(genericHandler, forbiddenHandler);

            ForbiddenAccessException exception = new ForbiddenAccessException("Access denied");

            ErrorResponse response = service.handleException(exception, TEST_PATH, TEST_TRACE_ID);

            assertThat(response.status()).isEqualTo(403);
            assertThat(response.error()).isEqualTo("FORBIDDEN_ACCESS");
        }

        @Test
        @DisplayName("should route ConstraintViolationException to ValidationExceptionHandler")
        void shouldRouteValidationException() {
            ValidationExceptionHandler validationHandler = new ValidationExceptionHandler();
            GenericExceptionHandler genericHandler = new GenericExceptionHandler();
            configureHandlers(genericHandler, validationHandler);

            ConstraintViolationException exception = mock(ConstraintViolationException.class);

            ErrorResponse response = service.handleException(exception, TEST_PATH, TEST_TRACE_ID);

            assertThat(response.status()).isEqualTo(400);
            assertThat(response.error()).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("should use GenericExceptionHandler as fallback for unknown exceptions")
        void shouldUseGenericHandlerAsFallback() {
            GenericExceptionHandler genericHandler = new GenericExceptionHandler();
            configureHandlers(genericHandler);

            RuntimeException exception = new RuntimeException("Unexpected error");

            ErrorResponse response = service.handleException(exception, TEST_PATH, TEST_TRACE_ID);

            assertThat(response.status()).isEqualTo(500);
            assertThat(response.error()).isEqualTo("INTERNAL_SERVER_ERROR");
        }

        @Test
        @DisplayName("should include path and traceId in response")
        void shouldIncludePathAndTraceId() {
            GenericExceptionHandler genericHandler = new GenericExceptionHandler();
            configureHandlers(genericHandler);

            RuntimeException exception = new RuntimeException("Error");

            ErrorResponse response = service.handleException(exception, TEST_PATH, TEST_TRACE_ID);

            assertThat(response.path()).isEqualTo(TEST_PATH);
            assertThat(response.traceId()).isEqualTo(TEST_TRACE_ID);
        }
    }

    @Nested
    @DisplayName("Handler Priority Selection")
    class HandlerPrioritySelection {

        @Test
        @DisplayName("should select handler with highest priority when multiple handlers match")
        void shouldSelectHighestPriorityHandler() {
            ExceptionHandler lowPriority = mock(ExceptionHandler.class);
            when(lowPriority.canHandle(any())).thenReturn(true);
            when(lowPriority.priority()).thenReturn(1);

            ExceptionHandler highPriority = mock(ExceptionHandler.class);
            when(highPriority.canHandle(any())).thenReturn(true);
            when(highPriority.priority()).thenReturn(10);
            when(highPriority.handle(any(), anyString(), anyString()))
                    .thenReturn(ErrorResponse.of(418, "HIGH_PRIORITY", "High priority handler", TEST_PATH, TEST_TRACE_ID));

            configureHandlers(lowPriority, highPriority);

            RuntimeException exception = new RuntimeException("Test");

            ErrorResponse response = service.handleException(exception, TEST_PATH, TEST_TRACE_ID);

            assertThat(response.error()).isEqualTo("HIGH_PRIORITY");
            verify(highPriority).handle(exception, TEST_PATH, TEST_TRACE_ID);
            verify(lowPriority, never()).handle(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("should verify real handlers have correct priorities")
        void shouldVerifyRealHandlerPriorities() {
            assertThat(new AuthenticationFailedHandler().priority()).isEqualTo(10);
            assertThat(new ForbiddenAccessHandler().priority()).isEqualTo(10);
            assertThat(new ValidationExceptionHandler().priority()).isEqualTo(8);
            assertThat(new GenericExceptionHandler().priority()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should throw IllegalStateException when no handler found")
        void shouldThrowWhenNoHandlerFound() {
            ExceptionHandler nonMatchingHandler = mock(ExceptionHandler.class);
            when(nonMatchingHandler.canHandle(any())).thenReturn(false);
            configureHandlers(nonMatchingHandler);

            RuntimeException exception = new RuntimeException("Unhandled");

            assertThatThrownBy(() -> service.handleException(exception, TEST_PATH, TEST_TRACE_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No exception handler found");
        }

        @Test
        @DisplayName("should work with empty handler list")
        void shouldThrowWithEmptyHandlerList() {
            configureHandlers();

            RuntimeException exception = new RuntimeException("Test");

            assertThatThrownBy(() -> service.handleException(exception, TEST_PATH, TEST_TRACE_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No exception handler found");
        }
    }

    @Nested
    @DisplayName("ErrorResponse Creation")
    class ErrorResponseCreation {

        @Test
        @DisplayName("should create ErrorResponse with all fields populated")
        void shouldCreateCompleteErrorResponse() {
            ErrorResponse response = ErrorResponse.of(
                    401,
                    "AUTHENTICATION_FAILED",
                    "Invalid token",
                    "/api/users",
                    "trace-123"
            );

            assertThat(response.status()).isEqualTo(401);
            assertThat(response.error()).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(response.message()).isEqualTo("Invalid token");
            assertThat(response.path()).isEqualTo("/api/users");
            assertThat(response.traceId()).isEqualTo("trace-123");
            assertThat(response.id()).isNotBlank();
            assertThat(response.timestamp()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should create ErrorResponse without traceId")
        void shouldCreateErrorResponseWithoutTraceId() {
            ErrorResponse response = ErrorResponse.of(
                    500,
                    "INTERNAL_ERROR",
                    "Something went wrong",
                    "/api/resource"
            );

            assertThat(response.traceId()).isNull();
            assertThat(response.status()).isEqualTo(500);
        }
    }
}

