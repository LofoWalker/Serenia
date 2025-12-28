package com.lofo.serenia.rest.dto.out;
import java.time.LocalDateTime;
/**
 * DTO représentant le statut de subscription d'un utilisateur.
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
        LocalDateTime dailyResetDate
) {
}
