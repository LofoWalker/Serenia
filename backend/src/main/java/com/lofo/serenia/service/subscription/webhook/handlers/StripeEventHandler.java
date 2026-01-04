package com.lofo.serenia.service.subscription.webhook.handlers;

import com.lofo.serenia.service.subscription.StripeEventType;
import com.stripe.model.Event;

/**
 * Interface for Stripe webhook event handlers.
 * Each handler implementation is responsible for processing a specific event type.
 * This design enables loose coupling and easy extensibility.
 */
public interface StripeEventHandler {

    /**
     * Returns the event type this handler is responsible for.
     *
     * @return the StripeEventType this handler processes
     */
    StripeEventType getEventType();

    /**
     * Processes the given Stripe event.
     * Implementation must be idempotent to handle webhook retries safely.
     *
     * @param event the Stripe event to process
     * @throws com.lofo.serenia.exception.exceptions.WebhookProcessingException
     *         for expected business errors (e.g., subscription not found)
     */
    void handle(Event event);
}

