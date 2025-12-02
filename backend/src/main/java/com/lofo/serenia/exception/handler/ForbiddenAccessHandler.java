package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.ForbiddenAccessException;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Handles forbidden access errors (403 Forbidden).
 */
@ApplicationScoped
public class ForbiddenAccessHandler implements ExceptionHandler {

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ForbiddenAccessException;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        return ErrorResponse.of(
                getStatus().getStatusCode(),
                "FORBIDDEN_ACCESS",
                exception.getMessage() != null ? exception.getMessage() : "Access to this resource is forbidden",
                path,
                traceId
        );
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.FORBIDDEN;
    }

    @Override
    public int priority() {
        return 10;
    }
}

