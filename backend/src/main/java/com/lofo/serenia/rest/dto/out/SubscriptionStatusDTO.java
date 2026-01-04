package com.lofo.serenia.rest.dto.out;

import java.time.LocalDateTime;

/**
 * DTO representing a user's subscription status.
 * Includes discount information if an active promotion code discount applies.
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
        boolean hasStripeSubscription,
        // Discount information - all nullable, only present if discount is active
        boolean hasActiveDiscount,
        String discountDescription,
        LocalDateTime discountEndDate
) {
}


