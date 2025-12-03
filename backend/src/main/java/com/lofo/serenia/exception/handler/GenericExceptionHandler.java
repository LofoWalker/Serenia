package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

/**
 * Handles all generic/unexpected exceptions (500 Internal Server Error).
 * This is the fallback handler with lowest priority.
 */
@ApplicationScoped
public class GenericExceptionHandler implements ExceptionHandler {

    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String AN_UNEXPECTED_ERROR_OCCURRED_PLEASE_CONTACT_SUPPORT_IF_THE_PROBLEM_PERSISTS = "An unexpected error occurred. Please contact support if the problem persists.";

    @Override
    public boolean canHandle(Throwable exception) {
        return true;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        return ErrorResponse.of(
                getStatus().getStatusCode(),
                INTERNAL_SERVER_ERROR,
                AN_UNEXPECTED_ERROR_OCCURRED_PLEASE_CONTACT_SUPPORT_IF_THE_PROBLEM_PERSISTS,
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
        return 0;
    }
}

