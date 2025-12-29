package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Stripe webhook event handler service.
 * Processes events sent by Stripe to maintain synchronization between Stripe and our database.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class StripeWebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    /**
     * Main entry point for processing Stripe events.
     *
     * @param event the Stripe event to process
     * @throws WebhookProcessingException for expected business errors (e.g., subscription not found)
     */
    @Transactional
    public void handleEvent(Event event) {
        String eventType = event.getType();

        switch (eventType) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
            case "customer.subscription.created" -> handleSubscriptionCreated(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
            default -> log.debug("Unhandled event type: {}", eventType);
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = deserializeObject(event, Session.class);
        if (session == null) {
            throw new WebhookProcessingException("Failed to deserialize checkout session");
        }

        String customerId = session.getCustomer();
        String stripeSubscriptionId = session.getSubscription();

        log.info("Checkout session completed for customer: {}, subscription: {}",
                customerId, stripeSubscriptionId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find("stripeCustomerId", customerId)
                .firstResultOptional();

        if (subscriptionOpt.isEmpty()) {
            throw new WebhookProcessingException("No subscription found for Stripe customer: " + customerId);
        }

        Subscription subscription = subscriptionOpt.get();

        if (subscription.getStripeSubscriptionId() == null) {
            subscription.setStripeSubscriptionId(stripeSubscriptionId);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.persist(subscription);
        }
    }

    private void handleSubscriptionCreated(Event event) {
        com.stripe.model.Subscription stripeSubscription = deserializeObject(event, com.stripe.model.Subscription.class);
        if (stripeSubscription == null) {
            throw new WebhookProcessingException("Failed to deserialize subscription");
        }

        String customerId = stripeSubscription.getCustomer();
        log.info("Subscription created for customer: {}", customerId);

        updateSubscriptionFromStripe(customerId, stripeSubscription);
    }

    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSubscription = deserializeObject(event, com.stripe.model.Subscription.class);
        if (stripeSubscription == null) {
            throw new WebhookProcessingException("Failed to deserialize subscription");
        }

        String customerId = stripeSubscription.getCustomer();
        log.info("Subscription updated for customer: {}", customerId);

        updateSubscriptionFromStripe(customerId, stripeSubscription);
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription = deserializeObject(event, com.stripe.model.Subscription.class);
        if (stripeSubscription == null) {
            throw new WebhookProcessingException("Failed to deserialize subscription");
        }

        String customerId = stripeSubscription.getCustomer();
        log.info("Subscription deleted for customer: {}, returning to FREE plan", customerId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find("stripeCustomerId", customerId)
                .firstResultOptional();

        if (subscriptionOpt.isEmpty()) {
            throw new WebhookProcessingException("No subscription found for customer: " + customerId);
        }

        Subscription subscription = subscriptionOpt.get();

        Plan freePlan = planRepository.getFreePlan();
        subscription.setPlan(freePlan);
        subscription.setStripeSubscriptionId(null);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCurrentPeriodEnd(null);

        subscriptionRepository.persist(subscription);
        log.info("User {} returned to FREE plan", subscription.getUser().getId());
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null) {
            throw new WebhookProcessingException("Failed to deserialize invoice");
        }

        String customerId = invoice.getCustomer();
        String subscriptionId = invoice.getSubscription();

        log.info("Payment succeeded for customer: {}, subscription: {}", customerId, subscriptionId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find("stripeCustomerId", customerId)
                .firstResultOptional();

        if (subscriptionOpt.isEmpty()) {
            throw new WebhookProcessingException("No subscription found for customer: " + customerId);
        }

        Subscription subscription = subscriptionOpt.get();

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.persist(subscription);
            log.info("Subscription status updated to ACTIVE after successful payment");
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null) {
            throw new WebhookProcessingException("Failed to deserialize invoice");
        }

        String customerId = invoice.getCustomer();
        log.warn("Payment failed for customer: {}", customerId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find("stripeCustomerId", customerId)
                .firstResultOptional();

        if (subscriptionOpt.isEmpty()) {
            throw new WebhookProcessingException("No subscription found for customer: " + customerId);
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.persist(subscription);

        log.info("Subscription status updated to PAST_DUE for user {}", subscription.getUser().getId());
    }

    private void updateSubscriptionFromStripe(String customerId, com.stripe.model.Subscription stripeSubscription) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find("stripeCustomerId", customerId)
                .firstResultOptional();

        if (subscriptionOpt.isEmpty()) {
            throw new WebhookProcessingException("No subscription found for customer: " + customerId);
        }

        Subscription subscription = subscriptionOpt.get();

        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());

        if (stripeSubscription.getCurrentPeriodEnd() != null) {
            subscription.setCurrentPeriodEnd(
                    LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()),
                            ZoneId.systemDefault()
                    )
            );
        }

        updatePlanFromStripeSubscription(subscription, stripeSubscription);

        subscriptionRepository.persist(subscription);
    }

    private void updatePlanFromStripeSubscription(Subscription subscription, com.stripe.model.Subscription stripeSubscription) {
        if (stripeSubscription.getItems() == null || stripeSubscription.getItems().getData().isEmpty()) {
            return;
        }

        String priceId = stripeSubscription.getItems().getData().getFirst().getPrice().getId();

        Optional<Plan> planOpt = planRepository
                .find("stripePriceId", priceId)
                .firstResultOptional();

        if (planOpt.isPresent()) {
            Plan newPlan = planOpt.get();
            if (!newPlan.equals(subscription.getPlan())) {
                log.info("Updating plan from {} to {} for customer {}",
                        subscription.getPlan().getName(), newPlan.getName(), subscription.getStripeCustomerId());
                subscription.setPlan(newPlan);
            }
        } else {
            log.warn("No plan found for Stripe price ID: {}", priceId);
        }
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired", "unpaid" -> SubscriptionStatus.UNPAID;
            case "trialing" -> SubscriptionStatus.ACTIVE;
            default -> {
                log.warn("Unknown Stripe status: {}, defaulting to ACTIVE", stripeStatus);
                yield SubscriptionStatus.ACTIVE;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> T deserializeObject(Event event, Class<T> clazz) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (clazz.isInstance(obj)) {
                return (T) obj;
            }
            log.error("Expected {} but got {}", clazz.getSimpleName(), obj.getClass().getSimpleName());
            return null;
        }

        try {
            StripeObject obj = deserializer.deserializeUnsafe();
            if (clazz.isInstance(obj)) {
                log.debug("Used unsafe deserialization for event: {}", event.getId());
                return (T) obj;
            }
            log.error("Expected {} but got {} (unsafe)", clazz.getSimpleName(), obj.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.error("Failed to deserialize event data for event: {} - {}", event.getId(), e.getMessage());
            return null;
        }
    }
}

