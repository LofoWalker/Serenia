package com.lofo.serenia.service.subscription;

import lombok.Getter;

/**
 * Stripe webhook event types handled by the application.
 * Each enum value corresponds to a Stripe event type string.
 *
 * This enum is used to:
 * - Map string event types from Stripe to type-safe enum values
 * - Route events to the appropriate handler methods
 * - Prevent string literal errors in event type switching
 */
@Getter
public enum StripeEventType {
    CHECKOUT_SESSION_COMPLETED("checkout.session.completed"),
    CHECKOUT_SESSION_EXPIRED("checkout.session.expired"),
    SUBSCRIPTION_CREATED("customer.subscription.created"),
    SUBSCRIPTION_UPDATED("customer.subscription.updated"),
    SUBSCRIPTION_DELETED("customer.subscription.deleted"),
    INVOICE_PAYMENT_SUCCEEDED("invoice.payment_succeeded"),
    INVOICE_PAYMENT_FAILED("invoice.payment_failed"),
    INVOICE_PAID("invoice.paid");

    private final String eventTypeString;

    StripeEventType(String eventTypeString) {
        this.eventTypeString = eventTypeString;
    }

    /**
     * Converts a Stripe event type string to the corresponding enum value.
     * Used when processing incoming webhook events.
     *
     * @param eventTypeString the Stripe event type string
     * @return the corresponding enum value, or empty Optional if not recognized
     */
    public static java.util.Optional<StripeEventType> fromString(String eventTypeString) {
        return java.util.Arrays.stream(values())
                .filter(e -> e.eventTypeString.equals(eventTypeString))
                .findFirst();
    }
}

