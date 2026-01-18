package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenericExceptionHandler Unit Tests")
class GenericExceptionHandlerTest {

    private GenericExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GenericExceptionHandler();
    }

    @Nested
    @DisplayName("canHandle")
    class CanHandle {

        @Test
        @DisplayName("should return true for any exception")
        void should_return_true_for_any_exception() {
            assertThat(handler.canHandle(new RuntimeException("Error"))).isTrue();
            assertThat(handler.canHandle(new IllegalArgumentException("Invalid"))).isTrue();
            assertThat(handler.canHandle(new NullPointerException())).isTrue();
            assertThat(handler.canHandle(new Exception("Generic"))).isTrue();
        }
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return internal server error")
        void should_return_internal_server_error() {
            RuntimeException exception = new RuntimeException("Something went wrong");

            ErrorResponse response = handler.handle(exception, "/api/resource", "trace-456");

            assertThat(response.status()).isEqualTo(500);
            assertThat(response.error()).isEqualTo("INTERNAL_SERVER_ERROR");
        }

        @Test
        @DisplayName("should include trace id")
        void should_include_trace_id() {
            RuntimeException exception = new RuntimeException("Error");
            String traceId = "trace-789";

            ErrorResponse response = handler.handle(exception, "/api/test", traceId);

            assertThat(response.traceId()).isEqualTo(traceId);
            assertThat(response.path()).isEqualTo("/api/test");
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return 500")
        void should_return_500() {
            Response.Status status = handler.getStatus();

            assertThat(status).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR);
            assertThat(status.getStatusCode()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("priority")
    class Priority {

        @Test
        @DisplayName("should return lowest priority")
        void should_return_lowest_priority() {
            int priority = handler.priority();

            assertThat(priority).isEqualTo(0);
        }
    }
}
