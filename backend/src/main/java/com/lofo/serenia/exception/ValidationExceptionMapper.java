package com.lofo.serenia.exception;

import com.lofo.serenia.rest.dto.out.ApiMessageResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.stream.Collectors;

/**
 * Exception mapper for validation constraint violations.
 *
 * Converts jakarta.validation ConstraintViolationException to a user-friendly HTTP 400 response.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(ValidationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        LOG.debugf("ValidationExceptionMapper: handling %d constraint violations",
                   exception.getConstraintViolations().size());

        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        LOG.debugf("ValidationExceptionMapper: violations - %s", message);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiMessageResponse("Erreur de validation: " + message))
                .build();
    }
}

