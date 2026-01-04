package com.lofo.serenia.service.subscription.webhook.handlers.subscription;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.discount.DiscountProcessor;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles customer.subscription.deleted events.
 * Downgraded the user to the FREE plan and clears subscription data.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubscriptionDeletedHandler implements StripeEventHandler {

    private static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StripeObjectMapper stripeObjectMapper;
    private final DiscountProcessor discountProcessor;

    @Override
    public StripeEventType getEventType() {
        return StripeEventType.SUBSCRIPTION_DELETED;
    }

    @Override
    public void handle(Event event) {
        com.stripe.model.Subscription stripeSubscription =
                stripeObjectMapper.deserialize(event, com.stripe.model.Subscription.class);

        String customerId = stripeSubscription.getCustomer();
        log.info("Subscription deleted for customer: {}, returning to FREE plan", customerId);

        Subscription subscription = findSubscriptionByCustomerId(customerId);

        Plan freePlan = planRepository.getFreePlan();
        subscription.setPlan(freePlan);
        subscription.setStripeSubscriptionId(null);
        subscription.setStripeCustomerId(null);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCurrentPeriodEnd(null);
        discountProcessor.clearDiscount(subscription);

        subscriptionRepository.persist(subscription);
        log.info("User {} returned to FREE plan after subscription deletion", subscription.getUser().getId());
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

