package com.lofo.serenia.exception;

import com.lofo.serenia.exception.model.ErrorResponse;
import com.lofo.serenia.exception.service.ExceptionHandlerService;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private ExceptionHandlerService handlerService;

    @Mock
    private UriInfo uriInfo;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() throws Exception {
        globalExceptionHandler = new GlobalExceptionHandler();

        Field handlerServiceField = GlobalExceptionHandler.class.getDeclaredField("handlerService");
        handlerServiceField.setAccessible(true);
        handlerServiceField.set(globalExceptionHandler, handlerService);

        Field uriInfoField = GlobalExceptionHandler.class.getDeclaredField("uriInfo");
        uriInfoField.setAccessible(true);
        uriInfoField.set(globalExceptionHandler, uriInfo);
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should delegate to handler service")
        void should_delegate_to_handler_service() {
            RuntimeException exception = new RuntimeException("Test error");
            ErrorResponse errorResponse = ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Error", "/api/test", "trace-123");

            when(uriInfo.getPath()).thenReturn("/api/test");
            when(handlerService.handleException(eq(exception), anyString(), anyString())).thenReturn(errorResponse);

            globalExceptionHandler.toResponse(exception);

            verify(handlerService).handleException(eq(exception), eq("/api/test"), anyString());
        }

        @Test
        @DisplayName("should generate trace id")
        void should_generate_trace_id() {
            RuntimeException exception = new RuntimeException("Test error");
            ErrorResponse errorResponse = ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Error", "/api/test", "trace-123");

            when(uriInfo.getPath()).thenReturn("/api/test");
            when(handlerService.handleException(any(), anyString(), anyString())).thenReturn(errorResponse);

            globalExceptionHandler.toResponse(exception);

            verify(handlerService).handleException(any(), anyString(), org.mockito.ArgumentMatchers.argThat(traceId ->
                    traceId != null && !traceId.isEmpty()
            ));
        }

        @Test
        @DisplayName("should extract request path")
        void should_extract_request_path() {
            RuntimeException exception = new RuntimeException("Test error");
            String expectedPath = "/api/users/profile";
            ErrorResponse errorResponse = ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Error", expectedPath, "trace-123");

            when(uriInfo.getPath()).thenReturn(expectedPath);
            when(handlerService.handleException(any(), eq(expectedPath), anyString())).thenReturn(errorResponse);

            globalExceptionHandler.toResponse(exception);

            verify(handlerService).handleException(any(), eq(expectedPath), anyString());
        }

        @Test
        @DisplayName("should return json response")
        void should_return_json_response() {
            RuntimeException exception = new RuntimeException("Test error");
            ErrorResponse errorResponse = ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Error", "/api/test", "trace-123");

            when(uriInfo.getPath()).thenReturn("/api/test");
            when(handlerService.handleException(any(), anyString(), anyString())).thenReturn(errorResponse);

            Response response = globalExceptionHandler.toResponse(exception);

            assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isEqualTo(errorResponse);
        }

        @Test
        @DisplayName("should use unknown path when uriInfo is null")
        void should_use_unknown_path_when_uriInfo_is_null() throws Exception {
            Field uriInfoField = GlobalExceptionHandler.class.getDeclaredField("uriInfo");
            uriInfoField.setAccessible(true);
            uriInfoField.set(globalExceptionHandler, null);

            RuntimeException exception = new RuntimeException("Test error");
            ErrorResponse errorResponse = ErrorResponse.of(500, "INTERNAL_SERVER_ERROR", "Error", "/unknown", "trace-123");

            when(handlerService.handleException(any(), eq("/unknown"), anyString())).thenReturn(errorResponse);

            globalExceptionHandler.toResponse(exception);

            verify(handlerService).handleException(any(), eq("/unknown"), anyString());
        }
    }
}
