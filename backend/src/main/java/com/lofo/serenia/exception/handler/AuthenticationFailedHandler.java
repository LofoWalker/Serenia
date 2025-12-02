package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.AuthenticationFailedException;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Handles authentication failures (401 Unauthorized).
 */
@ApplicationScoped
public class AuthenticationFailedHandler implements ExceptionHandler {

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof AuthenticationFailedException;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        return ErrorResponse.of(
                getStatus().getStatusCode(),
                "AUTHENTICATION_FAILED",
                exception.getMessage() != null ? exception.getMessage() : "Authentication credentials are invalid or missing",
                path,
                traceId
        );
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.UNAUTHORIZED;
    }

    @Override
    public int priority() {
        return 10;
    }
}

