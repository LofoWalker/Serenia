package com.lofo.serenia.service.subscription.webhook.handlers.invoice;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles invoice.payment_failed events.
 * Marks subscription as PAST_DUE when payment fails.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class InvoicePaymentFailedHandler implements StripeEventHandler {

    private static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";

    private final SubscriptionRepository subscriptionRepository;
    private final StripeObjectMapper stripeObjectMapper;

    @Override
    public StripeEventType getEventType() {
        return StripeEventType.INVOICE_PAYMENT_FAILED;
    }

    @Override
    public void handle(Event event) {
        Invoice invoice = stripeObjectMapper.deserialize(event, Invoice.class);

        String customerId = invoice.getCustomer();
        log.warn("Payment failed for customer: {}", customerId);

        Subscription subscription = findSubscriptionByCustomerId(customerId);
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.persist(subscription);

        log.info("Subscription status updated to PAST_DUE for user {}", subscription.getUser().getId());
    }

    /**
     * Finds a subscription by Stripe customer ID.
     *
     * @param customerId the Stripe customer ID
     * @return the subscription
     * @throws WebhookProcessingException if subscription not found
     */
    private Subscription findSubscriptionByCustomerId(String customerId) {
        return subscriptionRepository
                .find(STRIPE_CUSTOMER_ID, customerId)
                .firstResultOptional()
                .orElseThrow(() ->
                    new WebhookProcessingException("No subscription found for Stripe customer: " + customerId)
                );
    }
}

