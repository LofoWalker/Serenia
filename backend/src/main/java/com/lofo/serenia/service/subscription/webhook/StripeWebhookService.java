package com.lofo.serenia.service.subscription.webhook;

import com.lofo.serenia.exception.exceptions.WebhookHandlerNotFoundException;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Main Stripe webhook dispatcher service.
 * Routes incoming events to appropriate handlers based on event type.
 * Maintains a registry of handlers for clean extensibility.
 *
 * This service handles:
 * - Event routing via StripeEventType enum
 * - Handler lookup and delegation
 * - Transaction management
 *
 * Business logic is delegated to specific StripeEventHandler implementations.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class StripeWebhookService {

    private final Instance<StripeEventHandler> handlers;

    /**
     * Main entry point for processing Stripe webhook events.
     * Deserializes the event, finds the appropriate handler, and delegates processing.
     *
     * @param event the Stripe event to process
     * @throws com.lofo.serenia.exception.exceptions.WebhookProcessingException
     *         for expected business errors (e.g., subscription not found)
     */
    @Transactional
    public void handleEvent(Event event) {
        StripeEventType.fromString(event.getType())
                .ifPresentOrElse(
                        eventType -> delegateToHandler(eventType, event),
                        () -> log.debug("Unhandled event type: {}", event.getType())
                );
    }

    /**
     * Finds and delegates event processing to the appropriate handler.
     *
     * @param eventType the typed event type
     * @param event the Stripe event to process
     * @throws WebhookHandlerNotFoundException if no handler is registered for the event type
     */
    private void delegateToHandler(StripeEventType eventType, Event event) {
        var handler = findHandlerForEventType(eventType);

        if (handler.isPresent()) {
            log.debug("Delegating event {} to handler {}", event.getId(), handler.get().getClass().getSimpleName());
            handler.get().handle(event);
        } else {
            String errorMsg = String.format("No handler registered for event type: %s (id: %s)", eventType, event.getId());
            log.error(errorMsg);
            throw new WebhookHandlerNotFoundException(errorMsg);
        }
    }

    /**
     * Finds a handler instance for the given event type.
     *
     * @param eventType the event type to find a handler for
     * @return an Optional containing the handler if found
     */
    private java.util.Optional<StripeEventHandler> findHandlerForEventType(StripeEventType eventType) {
        java.util.Objects.requireNonNull(handlers, "StripeEventHandler CDI instance must not be null");

        java.util.Iterator<StripeEventHandler> iterator = handlers.iterator();
        java.util.Objects.requireNonNull(iterator, "Iterator over StripeEventHandler instances must not be null");

        while (iterator.hasNext()) {
            StripeEventHandler handler = iterator.next();
            if (handler.getEventType() == eventType) {
                return java.util.Optional.of(handler);
            }
        }
        return java.util.Optional.empty();
    }
}

