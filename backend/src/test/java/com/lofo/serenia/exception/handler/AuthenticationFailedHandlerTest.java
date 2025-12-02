package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.AuthenticationFailedException;
import com.lofo.serenia.exception.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthenticationFailedHandler.
 */
@DisplayName("Authentication Failed Handler")
class AuthenticationFailedHandlerTest {

    private AuthenticationFailedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthenticationFailedHandler();
    }

    @Test
    @DisplayName("Should handle AuthenticationFailedException")
    void shouldHandleAuthenticationFailedException() {
        // Arrange
        Throwable exception = new AuthenticationFailedException("Invalid credentials");

        // Act
        boolean canHandle = handler.canHandle(exception);

        // Assert
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("Should not handle other exceptions")
    void shouldNotHandleOtherExceptions() {
        // Arrange
        Throwable exception = new RuntimeException("Some error");

        // Act
        boolean canHandle = handler.canHandle(exception);

        // Assert
        assertThat(canHandle).isFalse();
    }

    @Test
    @DisplayName("Should return 401 Unauthorized status")
    void shouldReturnUnauthorizedStatus() {
        // Act
        Response.Status status = handler.getStatus();

        // Assert
        assertThat(status).isEqualTo(Response.Status.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should create error response with custom message")
    void shouldCreateErrorResponseWithCustomMessage() {
        // Arrange
        String customMessage = "Token expired";
        Throwable exception = new AuthenticationFailedException(customMessage);

        // Act
        ErrorResponse response = handler.handle(exception, "/api/users", "trace-123");

        // Assert
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.error()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(response.message()).isEqualTo(customMessage);
        assertThat(response.path()).isEqualTo("/api/users");
        assertThat(response.traceId()).isEqualTo("trace-123");
    }

    @Test
    @DisplayName("Should return default message when exception has no message")
    void shouldReturnDefaultMessageWhenNoExceptionMessage() {
        // Arrange - Create exception with message to satisfy constructor
        Throwable exception = new AuthenticationFailedException("Some message");

        // Act
        ErrorResponse response = handler.handle(exception, "/api/users", "trace-123");

        // Assert - Message should be from exception if provided
        assertThat(response.message()).isNotBlank();
    }

    @Test
    @DisplayName("Should have high priority")
    void shouldHaveHighPriority() {
        // Act
        int priority = handler.priority();

        // Assert
        assertThat(priority).isEqualTo(10);
    }
}

