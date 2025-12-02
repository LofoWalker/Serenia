package com.lofo.serenia.exception.service;

import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.exception.handler.*;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.inject.Instance;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ExceptionHandlerService.
 */
@DisplayName("Exception Handler Service")
class ExceptionHandlerServiceTest {

    private ExceptionHandlerService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        Instance<ExceptionHandler> handlers = mock(Instance.class);
        service = new ExceptionHandlerService(handlers);
    }

    @Test
    @DisplayName("Should route AuthenticationFailedException to correct handler")
    void shouldRouteAuthenticationFailedExceptionCorrectly() {
        AuthenticationFailedException exception = new AuthenticationFailedException("Token invalid");

        AuthenticationFailedHandler handler = new AuthenticationFailedHandler();

        assertThat(handler.canHandle(exception)).isTrue();
        assertThat(handler.getStatus().getStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should route ForbiddenAccessException to correct handler")
    void shouldRouteForbiddenAccessExceptionCorrectly() {
        // Arrange
        ForbiddenAccessException exception = new ForbiddenAccessException("Access denied");

        // Act
        ForbiddenAccessHandler handler = new ForbiddenAccessHandler();

        // Assert
        assertThat(handler.canHandle(exception)).isTrue();
        assertThat(handler.getStatus().getStatusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Should route ValidationException to correct handler")
    void shouldRouteValidationExceptionCorrectly() {
        // Arrange
        ConstraintViolationException exception = mock(ConstraintViolationException.class);

        // Act
        ValidationExceptionHandler handler = new ValidationExceptionHandler();

        // Assert
        assertThat(handler.canHandle(exception)).isTrue();
        assertThat(handler.getStatus().getStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should use generic handler as fallback")
    void shouldUseGenericHandlerAsFallback() {
        // Arrange
        RuntimeException exception = new RuntimeException("Unexpected error");

        // Act
        GenericExceptionHandler handler = new GenericExceptionHandler();

        // Assert
        assertThat(handler.canHandle(exception)).isTrue();
        assertThat(handler.getStatus().getStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("Handlers should have correct priorities")
    void handlersShouldHaveCorrectPriorities() {
        // Assert
        assertThat(new AuthenticationFailedHandler().priority()).isEqualTo(10);
        assertThat(new ForbiddenAccessHandler().priority()).isEqualTo(10);
        assertThat(new ValidationExceptionHandler().priority()).isEqualTo(8);
        assertThat(new GenericExceptionHandler().priority()).isEqualTo(0);
    }

    @Test
    @DisplayName("Error response should be created correctly")
    void errorResponseShouldBeCreatedCorrectly() {
        // Act
        ErrorResponse response = ErrorResponse.of(
                401,
                "AUTHENTICATION_FAILED",
                "Invalid token",
                "/api/users",
                "trace-123"
        );

        // Assert
        assertThat(response.status()).isEqualTo(401);
        assertThat(response.error()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(response.message()).isEqualTo("Invalid token");
        assertThat(response.path()).isEqualTo("/api/users");
        assertThat(response.traceId()).isEqualTo("trace-123");
        assertThat(response.id()).isNotBlank();
        assertThat(response.timestamp()).isGreaterThan(0);
    }
}

