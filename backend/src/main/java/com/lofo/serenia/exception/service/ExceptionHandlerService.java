package com.lofo.serenia.exception.service;

import com.lofo.serenia.exception.handler.ExceptionHandler;
import com.lofo.serenia.exception.model.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;

import java.util.Comparator;
import java.util.stream.StreamSupport;

/**
 * Exception handler orchestrator.
 * Discovers and manages all ExceptionHandler implementations.
 * Routes exceptions to appropriate handlers based on priority and type.
 */
@AllArgsConstructor
@ApplicationScoped
public class ExceptionHandlerService {

    Instance<ExceptionHandler> handlers;

    /**
     * Finds the appropriate handler for an exception and processes it.
     *
     * @param exception the exception to handle
     * @param path      the request path
     * @param traceId   correlation ID for tracking
     * @return error response for the client
     */
    public ErrorResponse handleException(Throwable exception, String path, String traceId) {
        ExceptionHandler handler = findHandler(exception);
        return handler.handle(exception, path, traceId);
    }

    /**
     * Finds the handler with the highest priority that can handle this exception.
     */
    private ExceptionHandler findHandler(Throwable exception) {
        return StreamSupport.stream(handlers.spliterator(), false)
                .filter(handler -> handler.canHandle(exception))
                .max(Comparator.comparingInt(ExceptionHandler::priority))
                .orElseThrow(() -> new IllegalStateException("No exception handler found"));
    }
}

