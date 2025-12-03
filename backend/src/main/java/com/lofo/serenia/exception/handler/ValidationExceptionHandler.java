package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

/**
 * Handles validation constraint violations (400 Bad Request).
 */
@ApplicationScoped
public class ValidationExceptionHandler implements ExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String REQUEST_VALIDATION_FAILED_PLEASE_CHECK_YOUR_INPUT_PARAMETERS = "Request validation failed. Please check your input parameters.";

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof ConstraintViolationException;
    }

    @Override
    public ErrorResponse handle(Throwable exception, String path, String traceId) {
        return ErrorResponse.of(
                getStatus().getStatusCode(),
                VALIDATION_ERROR,
                REQUEST_VALIDATION_FAILED_PLEASE_CHECK_YOUR_INPUT_PARAMETERS,
                path,
                traceId
        );
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    public int priority() {
        return 8;
    }
}

