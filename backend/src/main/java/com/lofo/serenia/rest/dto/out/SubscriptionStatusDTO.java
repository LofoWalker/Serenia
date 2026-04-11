package com.lofo.serenia.rest.dto.out;

import java.time.Instant;

/**
 * DTO representing a user's subscription status.
 * Discount information is NOT tracked - it is fully auditable in Stripe.
 */
public record SubscriptionStatusDTO(
        String planName,
        int tokensRemainingThisMonth,
        int messagesRemainingToday,
        int monthlyTokenLimit,
        int dailyMessageLimit,
        int tokensUsedThisMonth,
        int messagesSentToday,
        Instant monthlyResetDate,
        Instant dailyResetDate,
        String status,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Integer priceCents,
        String currency,
        boolean hasStripeSubscription
) {}
