package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ValidationExceptionHandler.
 */
@DisplayName("Validation Exception Handler")
class ValidationExceptionHandlerTest {

    private ValidationExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ValidationExceptionHandler();
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException")
    void shouldHandleConstraintViolationException() {
        // Arrange
        ConstraintViolationException exception = mock(ConstraintViolationException.class);

        // Act
        boolean canHandle = handler.canHandle(exception);

        // Assert
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("Should return 400 Bad Request status")
    void shouldReturnBadRequestStatus() {
        // Act
        Response.Status status = handler.getStatus();

        // Assert
        assertThat(status).isEqualTo(Response.Status.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should create validation error response")
    void shouldCreateValidationErrorResponse() {
        // Arrange
        ConstraintViolationException exception = mock(ConstraintViolationException.class);

        // Act
        ErrorResponse response = handler.handle(exception, "/api/users", "trace-789");

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.message()).contains("validation failed");
    }

    @Test
    @DisplayName("Should have medium-high priority")
    void shouldHaveMediumHighPriority() {
        // Act
        int priority = handler.priority();

        // Assert
        assertThat(priority).isEqualTo(8);
    }
}

