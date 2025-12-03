package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Handles forbidden access errors (403 Forbidden).
 */
@ApplicationScoped
public class ForbiddenAccessHandler implements ExceptionHandler {

    private static final String FORBIDDEN_ACCESS = "FORBIDDEN_ACCESS";
    private static final String ACCESS_TO_THIS_RESOURCE_IS_FORBIDDEN = "Access to this resource is forbidden";

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ForbiddenAccessException;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        return ErrorResponse.of(
                getStatus().getStatusCode(),
                FORBIDDEN_ACCESS,
                exception.getMessage() != null ? exception.getMessage() : ACCESS_TO_THIS_RESOURCE_IS_FORBIDDEN,
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

