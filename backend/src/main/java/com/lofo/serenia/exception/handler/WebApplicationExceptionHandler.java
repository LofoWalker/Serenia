package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Handles JAX-RS WebApplicationException.
 * Provides appropriate error messages based on HTTP status codes.
 */
@ApplicationScoped
public class WebApplicationExceptionHandler implements ExceptionHandler {

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof WebApplicationException;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        WebApplicationException webException = (WebApplicationException) exception;
        int statusCode = webException.getResponse().getStatus();
        String errorType = getErrorType(statusCode);
        String message = getErrorMessage(statusCode);

        return ErrorResponse.of(
                statusCode,
                errorType,
                message,
                path,
                traceId
        );
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public int priority() {
        return 5;
    }

    private String getErrorType(int statusCode) {
        return switch (statusCode) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "UNPROCESSABLE_ENTITY";
            case 500, 502, 503, 504 -> "SERVER_ERROR";
            default -> "HTTP_ERROR";
        };
    }

    private String getErrorMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Invalid request format or parameters";
            case 404 -> "The requested resource was not found";
            case 409 -> "The request conflicts with existing data";
            case 422 -> "The request is syntactically correct but semantically invalid";
            case 500 -> "An internal server error occurred";
            case 502 -> "Bad gateway";
            case 503 -> "Service temporarily unavailable";
            case 504 -> "Gateway timeout";
            default -> "An HTTP error occurred";
        };
    }
}

