package com.lofo.serenia.service.subscription;

import com.lofo.serenia.config.StripeConfig;
import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.rest.dto.out.CheckoutSessionDTO;
import com.lofo.serenia.rest.dto.out.PortalSessionDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Stripe integration service.
 * Handles Checkout session creation, customer portal, and Stripe customer management.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfig stripeConfig;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserFinder userFinder;

    @PostConstruct
    void init() {
        Stripe.apiKey = stripeConfig.apiKey();
        log.info("Stripe SDK initialized");
    }

    /**
     * Creates a Stripe Checkout session for subscribing to a paid plan.
     *
     * @param userId   the user identifier
     * @param planType the desired plan type (PLUS or MAX)
     * @return DTO containing the redirect URL to Stripe Checkout
     * @throws SereniaException if the plan is FREE or a Stripe error occurs
     */
    @Transactional
    public CheckoutSessionDTO createCheckoutSession(UUID userId, PlanType planType) {
        validatePaidPlan(planType);

        User user = userFinder.findByIdOrThrow(userId);
        Subscription subscription = getOrThrowSubscription(userId);
        Plan targetPlan = getPlanOrThrow(planType);

        validatePlanHasStripePrice(targetPlan);

        String customerId = getOrCreateStripeCustomer(user, subscription);

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(stripeConfig.successUrl())
                    .setCancelUrl(stripeConfig.cancelUrl())
                    .setAllowPromotionCodes(true)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(targetPlan.getStripePriceId())
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("plan_type", planType.name())
                    .build();

            Session session = Session.create(params);

            log.info("Created Checkout session {} for user {} with plan {}",
                    session.getId(), userId, planType);

            return new CheckoutSessionDTO(session.getId(), session.getUrl());

        } catch (StripeException e) {
            log.error("Failed to create Checkout session for user {}: {}", userId, e.getMessage());
            throw SereniaException.internalError("Failed to create payment session: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a Stripe Customer Portal session for managing subscription.
     *
     * @param userId the user identifier
     * @return DTO containing the redirect URL to Stripe portal
     * @throws SereniaException if the user has no Stripe customer
     */
    @Transactional
    public PortalSessionDTO createPortalSession(UUID userId) {
        Subscription subscription = getOrThrowSubscription(userId);

        String customerId = subscription.getStripeCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            throw SereniaException.badRequest("No Stripe customer found. Please subscribe to a plan first.");
        }

        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(customerId)
                            .setReturnUrl(stripeConfig.successUrl().replace("?payment=success", ""))
                            .build();

            com.stripe.model.billingportal.Session portalSession =
                    com.stripe.model.billingportal.Session.create(params);

            log.info("Created Portal session for user {}", userId);

            return new PortalSessionDTO(portalSession.getUrl());

        } catch (StripeException e) {
            log.error("Failed to create Portal session for user {}: {}", userId, e.getMessage());
            throw SereniaException.internalError("Failed to create portal session: " + e.getMessage(), e);
        }
    }

    /**
     * Gets or creates a Stripe customer for the user.
     *
     * @param user         the user
     * @param subscription the user's subscription
     * @return the Stripe customer ID
     */
    @Transactional
    public String getOrCreateStripeCustomer(User user, Subscription subscription) {
        if (subscription.getStripeCustomerId() != null && !subscription.getStripeCustomerId().isEmpty()) {
            return subscription.getStripeCustomerId();
        }

        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getFirstName() + " " + user.getLastName())
                    .putMetadata("user_id", user.getId().toString())
                    .build();

            Customer customer = Customer.create(params);

            subscription.setStripeCustomerId(customer.getId());
            subscriptionRepository.persist(subscription);

            log.info("Created Stripe customer {} for user {}", customer.getId(), user.getId());

            return customer.getId();

        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for user {}: {}", user.getId(), e.getMessage());
            throw SereniaException.internalError("Failed to create customer: " + e.getMessage(), e);
        }
    }


    private void validatePaidPlan(PlanType planType) {
        if (planType == PlanType.FREE) {
            throw SereniaException.badRequest("Cannot checkout for FREE plan. FREE plan is default.");
        }
    }

    private void validatePlanHasStripePrice(Plan plan) {
        if (plan.getStripePriceId() == null || plan.getStripePriceId().isEmpty()) {
            throw SereniaException.badRequest("Plan " + plan.getName() + " has no Stripe Price ID configured");
        }
    }

    private Subscription getOrThrowSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> SereniaException.notFound("Subscription not found for user: " + userId));
    }

    private Plan getPlanOrThrow(PlanType planType) {
        return planRepository.findByName(planType)
                .orElseThrow(() -> SereniaException.notFound("Plan not found: " + planType));
    }
}

