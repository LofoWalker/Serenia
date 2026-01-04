package com.lofo.serenia.service.subscription.webhook.handlers.checkout;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles checkout.session.completed events.
 * Links the Stripe subscription ID to the internal subscription record.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CheckoutSessionCompletedHandler implements StripeEventHandler {

    private static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";

    private final SubscriptionRepository subscriptionRepository;
    private final StripeObjectMapper stripeObjectMapper;

    @Override
    public StripeEventType getEventType() {
        return StripeEventType.CHECKOUT_SESSION_COMPLETED;
    }

    @Override
    public void handle(Event event) {
        Session session = stripeObjectMapper.deserialize(event, Session.class);

        String customerId = session.getCustomer();
        String stripeSubscriptionId = session.getSubscription();

        log.info("Checkout session completed for customer: {}, subscription: {}",
                customerId, stripeSubscriptionId);

        Subscription subscription = findSubscriptionByCustomerId(customerId);

        if (subscription.getStripeSubscriptionId() == null) {
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.persist(subscription);
            log.info("Updated subscription with Stripe subscription ID: {}", stripeSubscriptionId);
        } else {
            log.debug("Subscription already has Stripe subscription ID, skipping update");
        }
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

