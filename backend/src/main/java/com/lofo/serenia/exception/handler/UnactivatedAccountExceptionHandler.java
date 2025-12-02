package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Handles unactivated account authentication failures (401 Unauthorized).
 */
@ApplicationScoped
public class UnactivatedAccountExceptionHandler implements ExceptionHandler {

    private static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    private static final String AUTHENTICATION_CREDENTIALS_ARE_INVALID_OR_MISSING = "Authentication credentials are invalid or missing";

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof AuthenticationFailedException;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        return ErrorResponse.of(
                getStatus().getStatusCode(),
                AUTHENTICATION_FAILED,
                exception.getMessage() != null ? exception.getMessage() : AUTHENTICATION_CREDENTIALS_ARE_INVALID_OR_MISSING,
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

