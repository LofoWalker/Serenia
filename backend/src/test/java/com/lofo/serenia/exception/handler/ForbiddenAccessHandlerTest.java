package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.ForbiddenAccessException;
import com.lofo.serenia.exception.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ForbiddenAccessHandler.
 */
@DisplayName("Forbidden Access Handler")
class ForbiddenAccessHandlerTest {

    private ForbiddenAccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ForbiddenAccessHandler();
    }

    @Test
    @DisplayName("Should handle ForbiddenAccessException")
    void shouldHandleForbiddenAccessException() {
        // Arrange
        Throwable exception = new ForbiddenAccessException("Access denied");

        // Act
        boolean canHandle = handler.canHandle(exception);

        // Assert
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("Should return 403 Forbidden status")
    void shouldReturnForbiddenStatus() {
        // Act
        Response.Status status = handler.getStatus();

        // Assert
        assertThat(status).isEqualTo(Response.Status.FORBIDDEN);
    }

    @Test
    @DisplayName("Should create error response with resource-specific message")
    void shouldCreateErrorResponseWithResourceMessage() {
        // Arrange
        String message = "Conversation does not belong to user";
        Throwable exception = new ForbiddenAccessException(message);

        // Act
        ErrorResponse response = handler.handle(exception, "/api/conversations/123", "trace-456");

        // Assert
        assertThat(response.status()).isEqualTo(403);
        assertThat(response.error()).isEqualTo("FORBIDDEN_ACCESS");
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.path()).isEqualTo("/api/conversations/123");
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

