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
            case "invoice.paid" -> handleInvoicePaid(event);
            case "checkout.session.expired" -> handleCheckoutSessionExpired(event);
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
            // Discount data will be extracted when customer.subscription.created/updated events fire
            // At this point, the Stripe subscription doesn't have the discount attached yet
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
        subscription.setStripeCustomerId(null);
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

        // Extract and update discount data from the subscription object
        // This is the source of truth once the subscription is created in Stripe.
        // It supersedes any discount extracted from the checkout session.
        if (stripeSubscription.getDiscount() != null) {
            StripeDiscountHelper.DiscountData discountData =
                    StripeDiscountHelper.extractDiscountData(stripeSubscription.getDiscount());
            if (discountData != null) {
                // Discount is currently active on the subscription
                subscription.setStripeCouponId(discountData.couponId());
                subscription.setDiscountType(discountData.type());
                subscription.setDiscountValue(discountData.value());
                subscription.setDiscountEndDate(discountData.endDate());
                log.info("Updated subscription discount: coupon={}, type={}, value={}, endDate={}",
                        discountData.couponId(), discountData.type(), discountData.value(), discountData.endDate());
            } else {
                // Discount was removed - clear all discount fields
                subscription.setStripeCouponId(null);
                subscription.setDiscountType(null);
                subscription.setDiscountValue(null);
                subscription.setDiscountEndDate(null);
                log.info("Discount removed from subscription");
            }
        } else if (subscription.getDiscountEndDate() != null &&
                   StripeDiscountHelper.isDiscountExpired(subscription.getDiscountEndDate())) {
            // Discount has expired based on our stored end date
            subscription.setStripeCouponId(null);
            subscription.setDiscountType(null);
            subscription.setDiscountValue(null);
            subscription.setDiscountEndDate(null);
            log.info("Discount expired for subscription {}", stripeSubscription.getId());
        } else if (stripeSubscription.getDiscount() == null && subscription.getStripeCouponId() != null) {
            // Subscription has no discount in Stripe but our DB has one - this means it was removed
            // Clear the discount data from our database
            subscription.setStripeCouponId(null);
            subscription.setDiscountType(null);
            subscription.setDiscountValue(null);
            subscription.setDiscountEndDate(null);
            log.info("Clearing expired or removed discount for subscription");
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

    private void handleInvoicePaid(Event event) {
        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null) {
            throw new WebhookProcessingException("Failed to deserialize invoice");
        }

        String customerId = invoice.getCustomer();
        long amountPaid = invoice.getAmountPaid();
        String currency = invoice.getCurrency();

        log.info("Invoice paid for customer: {}, amount: {} {}",
                customerId, amountPaid / 100.0, currency);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find("stripeCustomerId", customerId)
                .firstResultOptional();

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            // Store the last invoice amount paid and currency for auditability
            subscription.setLastInvoiceAmount((int) amountPaid);
            subscription.setCurrency(currency);

            // Log discount information if applicable
            if (subscription.getStripeCouponId() != null) {
                log.info("Invoice paid with active discount - coupon: {}, type: {}, value: {}, " +
                        "amountAfterDiscount: {} {}",
                        subscription.getStripeCouponId(),
                        subscription.getDiscountType(),
                        subscription.getDiscountValue(),
                        amountPaid / 100.0,
                        currency);
            }

            subscriptionRepository.persist(subscription);

            log.debug("Updated subscription with last invoice amount and currency");
        } else {
            log.warn("No subscription found for customer: {} when processing invoice.paid", customerId);
        }
    }

    private void handleCheckoutSessionExpired(Event event) {
        Session session = deserializeObject(event, Session.class);
        if (session == null) {
            throw new WebhookProcessingException("Failed to deserialize checkout session");
        }

        String customerId = session.getCustomer();
        String sessionId = session.getId();

        log.warn("Checkout session expired for customer: {}, sessionId: {}", customerId, sessionId);

        // Note: We don't need to update the subscription here since the checkout
        // was never completed. This event is logged for analytics/support purposes.
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

