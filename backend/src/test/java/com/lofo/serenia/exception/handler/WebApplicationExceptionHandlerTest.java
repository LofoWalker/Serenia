package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WebApplicationExceptionHandler.
 */
@DisplayName("Web Application Exception Handler")
class WebApplicationExceptionHandlerTest {

    private WebApplicationExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebApplicationExceptionHandler();
    }

    @Test
    @DisplayName("Should handle WebApplicationException")
    void shouldHandleWebApplicationException() {
        // Arrange
        WebApplicationException exception = mock(WebApplicationException.class);

        // Act
        boolean canHandle = handler.canHandle(exception);

        // Assert
        assertThat(canHandle).isTrue();
    }

    @Test
    @DisplayName("Should handle 404 Not Found with appropriate message")
    void shouldHandle404NotFound() {
        // Arrange
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(exception.getResponse()).thenReturn(response);

        // Act
        ErrorResponse errorResponse = handler.handle(exception, "/api/users/123", "trace-404");

        // Assert
        assertThat(errorResponse.status()).isEqualTo(404);
        assertThat(errorResponse.error()).isEqualTo("NOT_FOUND");
        assertThat(errorResponse.message()).contains("not found");
    }

    @Test
    @DisplayName("Should handle 400 Bad Request")
    void shouldHandle400BadRequest() {
        // Arrange
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(exception.getResponse()).thenReturn(response);

        // Act
        ErrorResponse errorResponse = handler.handle(exception, "/api/users", "trace-400");

        // Assert
        assertThat(errorResponse.status()).isEqualTo(400);
        assertThat(errorResponse.error()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("Should handle 409 Conflict")
    void shouldHandle409Conflict() {
        // Arrange
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(409);
        when(exception.getResponse()).thenReturn(response);

        // Act
        ErrorResponse errorResponse = handler.handle(exception, "/api/emails", "trace-409");

        // Assert
        assertThat(errorResponse.status()).isEqualTo(409);
        assertThat(errorResponse.error()).isEqualTo("CONFLICT");
        assertThat(errorResponse.message()).contains("conflicts");
    }

    @Test
    @DisplayName("Should handle 500 Internal Server Error")
    void shouldHandle500InternalServerError() {
        // Arrange
        WebApplicationException exception = mock(WebApplicationException.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(exception.getResponse()).thenReturn(response);

        // Act
        ErrorResponse errorResponse = handler.handle(exception, "/api/data", "trace-500");

        // Assert
        assertThat(errorResponse.status()).isEqualTo(500);
        assertThat(errorResponse.error()).isEqualTo("SERVER_ERROR");
    }
}

