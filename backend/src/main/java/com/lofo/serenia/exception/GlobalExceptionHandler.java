package com.lofo.serenia.exception;

import com.lofo.serenia.exception.model.ErrorResponse;
import com.lofo.serenia.exception.service.ExceptionHandlerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.UUID;

@Provider
@ApplicationScoped
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Inject
    ExceptionHandlerService handlerService;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        // Generate correlation ID for tracking this error
        String traceId = UUID.randomUUID().toString();
        String path = getRequestPath();

        LOG.error("Exception in path: " + path + " [traceId: " + traceId + "]", exception);

        // Delegate to handler service
        ErrorResponse errorResponse = handlerService.handleException(exception, path, traceId);

        // Build and return HTTP response
        return Response.status(errorResponse.status())
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Extracts the request path from the current request context.
     */
    private String getRequestPath() {
        if (uriInfo != null && uriInfo.getPath() != null) {
            return uriInfo.getPath();
        }
        return "/unknown";
    }
}

