package com.lofo.serenia.exception.exceptions;

/**
 * Exception thrown when a webhook event cannot be processed due to expected business conditions
 * (e.g., subscription not found, customer not found). These errors should not trigger Stripe retries.
 */
public class WebhookProcessingException extends RuntimeException {

    public WebhookProcessingException(String message) {
        super(message);
    }

    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

