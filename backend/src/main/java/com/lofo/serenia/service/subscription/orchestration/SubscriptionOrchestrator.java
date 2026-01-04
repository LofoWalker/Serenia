package com.lofo.serenia.service.subscription.orchestration;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.discount.DiscountProcessor;
import com.lofo.serenia.service.subscription.mapper.DateTimeConverter;
import com.lofo.serenia.service.subscription.mapper.StripeStatusMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Orchestrates subscription updates from Stripe data.
 * Coordinates synchronization of basic fields, discounts, and plans.
 * Provides a clear, single entry point for subscription synchronization logic.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubscriptionOrchestrator {

    private static final String STRIPE_PRICE_ID = "stripePriceId";

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StripeStatusMapper statusMapper;
    private final DateTimeConverter dateTimeConverter;
    private final DiscountProcessor discountProcessor;

    /**
     * Synchronizes a subscription with data from a Stripe subscription object.
     * Updates basic fields, discount information, and plan association.
     *
     * @param subscription the internal subscription to update
     * @param stripeSubscription the Stripe subscription object as source of truth
     */
    public void synchronizeFromStripe(
            Subscription subscription,
            com.stripe.model.Subscription stripeSubscription) {

        updateBasicFields(subscription, stripeSubscription);
        updateDiscount(subscription, stripeSubscription);
        updatePlan(subscription, stripeSubscription);

        subscriptionRepository.persist(subscription);
        log.debug("Subscription {} synchronized from Stripe", stripeSubscription.getId());
    }

    /**
     * Updates basic subscription fields from Stripe subscription.
     * Covers: ID, status, cancel-at-period-end flag, and period end date.
     */
    private void updateBasicFields(
            Subscription subscription,
            com.stripe.model.Subscription stripeSubscription) {

        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(statusMapper.mapStatus(stripeSubscription.getStatus()));
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());

        if (stripeSubscription.getCurrentPeriodEnd() != null) {
            subscription.setCurrentPeriodEnd(
                    dateTimeConverter.convertEpochToDateTime(stripeSubscription.getCurrentPeriodEnd())
            );
        }

        log.debug("Updated basic fields for subscription {}: status={}, cancelAtPeriodEnd={}",
                stripeSubscription.getId(),
                stripeSubscription.getStatus(),
                stripeSubscription.getCancelAtPeriodEnd());
    }

    /**
     * Updates discount information from Stripe subscription.
     * Handles three scenarios:
     * - Discount is currently active: applies it
     * - Discount has expired: clears expired data
     * - Discount was removed: clears any previous discount data
     */
    private void updateDiscount(
            Subscription subscription,
            com.stripe.model.Subscription stripeSubscription) {

        if (stripeSubscription.getDiscount() != null) {
            discountProcessor.applyDiscount(subscription, stripeSubscription.getDiscount());
        } else if (discountProcessor.isDiscountExpired(subscription)) {
            log.info("Discount expired for subscription {}", stripeSubscription.getId());
            discountProcessor.clearDiscount(subscription);
        } else if (subscription.getStripeCouponId() != null) {
            log.info("Clearing removed discount for subscription {}", stripeSubscription.getId());
            discountProcessor.clearDiscount(subscription);
        }
    }

    /**
     * Updates the plan association from Stripe subscription's price information.
     * Finds the internal plan by Stripe price ID and updates if changed.
     */
    private void updatePlan(
            Subscription subscription,
            com.stripe.model.Subscription stripeSubscription) {

        if (stripeSubscription.getItems() == null || stripeSubscription.getItems().getData().isEmpty()) {
            log.warn("Stripe subscription {} has no items", stripeSubscription.getId());
            return;
        }

        String priceId = stripeSubscription.getItems().getData().getFirst().getPrice().getId();

        Optional<Plan> planOpt = planRepository
                .find(STRIPE_PRICE_ID, priceId)
                .firstResultOptional();

        if (planOpt.isPresent()) {
            Plan newPlan = planOpt.get();
            if (!newPlan.equals(subscription.getPlan())) {
                log.info("Updating plan from {} to {} for subscription {}",
                        subscription.getPlan().getName(),
                        newPlan.getName(),
                        stripeSubscription.getId());
                subscription.setPlan(newPlan);
            }
        } else {
            log.warn("No plan found for Stripe price ID: {}", priceId);
        }
    }
}

