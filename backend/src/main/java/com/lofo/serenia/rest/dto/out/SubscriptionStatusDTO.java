package com.lofo.serenia.rest.dto.out;

import java.time.LocalDateTime;

/**
 * DTO representing a user's subscription status.
 * Discount information is NOT tracked - it is fully auditable in Stripe.
 */
public record SubscriptionStatusDTO(
        String planName,
        int tokensRemainingThisMonth,
        int messagesRemainingToday,
        int perMessageTokenLimit,
        int monthlyTokenLimit,
        int dailyMessageLimit,
        int tokensUsedThisMonth,
        int messagesSentToday,
        LocalDateTime monthlyResetDate,
        LocalDateTime dailyResetDate,
        String status,
        LocalDateTime currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Integer priceCents,
        String currency,
        boolean hasStripeSubscription
) {
}




