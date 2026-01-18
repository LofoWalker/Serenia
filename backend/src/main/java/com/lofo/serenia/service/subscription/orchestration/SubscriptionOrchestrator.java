package com.lofo.serenia.service.subscription.orchestration;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.mapper.DateTimeConverter;
import com.lofo.serenia.service.subscription.mapper.StripeStatusMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Orchestrates subscription updates from Stripe data.
 * Coordinates synchronization of basic fields and plans.
 * Provides a clear, single entry point for subscription synchronization logic.
 *
 * NOTE: Discounts are NOT tracked in the database because they are fully auditable
 * in Stripe. The source of truth for all discount information is Stripe itself.
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

    /**
     * Synchronizes a subscription with data from a Stripe subscription object.
     * Updates basic fields and plan association.
     * Discounts are not persisted - they are queried from Stripe when needed.
     *
     * @param subscription the internal subscription to update
     * @param stripeSubscription the Stripe subscription object as source of truth
     */
    public void synchronizeFromStripe(
            Subscription subscription,
            com.stripe.model.Subscription stripeSubscription) {

        updateBasicFields(subscription, stripeSubscription);
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

        Boolean cancelAtPeriodEnd = stripeSubscription.getCancelAtPeriodEnd();
        subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd != null ? cancelAtPeriodEnd : Boolean.FALSE);

        if (stripeSubscription.getCurrentPeriodEnd() != null) {
            subscription.setCurrentPeriodEnd(dateTimeConverter.convertEpochToDateTime(stripeSubscription.getCurrentPeriodEnd()));
        }

        log.debug("Updated basic fields for subscription {}: status={}, cancelAtPeriodEnd={}",
                stripeSubscription.getId(),
                stripeSubscription.getStatus(),
                stripeSubscription.getCancelAtPeriodEnd());
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



