package com.lofo.serenia.exception.exceptions;

/**
 * Exception thrown when a recognized Stripe webhook event type has no handler registered.
 * This indicates a configuration error and should trigger a Stripe retry (HTTP 500).
 * Unlike WebhookProcessingException, this is NOT an expected business condition but
 * rather a deployment or configuration issue that must be resolved.
 */
public class WebhookHandlerNotFoundException extends RuntimeException {

    public WebhookHandlerNotFoundException(String message) {
        super(message);
    }

    public WebhookHandlerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

