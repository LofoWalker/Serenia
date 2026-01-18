package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnactivatedAccountExceptionHandler Unit Tests")
class UnactivatedAccountHandlerTest {

    private UnactivatedAccountExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UnactivatedAccountExceptionHandler();
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        @DisplayName("should return true for authentication failed exception")
        void should_return_true_for_authentication_failed_exception() {
            AuthenticationFailedException exception = new AuthenticationFailedException("Account not activated");

            boolean result = handler.canHandle(exception);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for other exceptions")
        void should_return_false_for_other_exceptions() {
            RuntimeException exception = new RuntimeException("Some error");

            boolean result = handler.canHandle(exception);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return unauthorized error response")
        void should_return_unauthorized_error_response() {
            AuthenticationFailedException exception = new AuthenticationFailedException("Account not activated");

            ErrorResponse response = handler.handle(exception, "/api/auth/login", "trace-123");

            assertThat(response.status()).isEqualTo(401);
            assertThat(response.error()).isEqualTo("AUTHENTICATION_FAILED");
            assertThat(response.message()).isEqualTo("Account not activated");
            assertThat(response.path()).isEqualTo("/api/auth/login");
            assertThat(response.traceId()).isEqualTo("trace-123");
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return 401 unauthorized")
        void should_return_401_unauthorized() {
            Response.Status status = handler.getStatus();

            assertThat(status).isEqualTo(Response.Status.UNAUTHORIZED);
            assertThat(status.getStatusCode()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("priority")
    class Priority {

        @Test
        @DisplayName("should return priority 10")
        void should_return_priority_10() {
            int priority = handler.priority();

            assertThat(priority).isEqualTo(10);
        }
    }
}
