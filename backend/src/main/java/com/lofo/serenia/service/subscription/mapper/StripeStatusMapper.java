package com.lofo.serenia.service.subscription.mapper;

import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps Stripe subscription status strings to internal SubscriptionStatus enum.
 * Isolates status mapping logic for reusability and testability.
 */
@Slf4j
@ApplicationScoped
public class StripeStatusMapper {

    /**
     * Converts a Stripe subscription status string to the internal SubscriptionStatus enum.
     *
     * @param stripeStatus the Stripe status string (e.g., "active", "past_due")
     * @return the corresponding SubscriptionStatus enum value
     */
    public SubscriptionStatus mapStatus(String stripeStatus) {
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
}

