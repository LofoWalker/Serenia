package com.lofo.serenia.service.subscription.webhook.handlers.subscription;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.orchestration.SubscriptionOrchestrator;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles customer.subscription.created events.
 * Synchronizes new subscription data from Stripe.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubscriptionCreatedHandler implements StripeEventHandler {

    private static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";

    private final SubscriptionRepository subscriptionRepository;
    private final StripeObjectMapper stripeObjectMapper;
    private final SubscriptionOrchestrator subscriptionOrchestrator;

    @Override
    public StripeEventType getEventType() {
        return StripeEventType.SUBSCRIPTION_CREATED;
    }

    @Override
    public void handle(Event event) {
        com.stripe.model.Subscription stripeSubscription =
                stripeObjectMapper.deserialize(event, com.stripe.model.Subscription.class);

        String customerId = stripeSubscription.getCustomer();
        log.info("Subscription created for customer: {}", customerId);

        Subscription subscription = findSubscriptionByCustomerId(customerId);
        subscriptionOrchestrator.synchronizeFromStripe(subscription, stripeSubscription);
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

