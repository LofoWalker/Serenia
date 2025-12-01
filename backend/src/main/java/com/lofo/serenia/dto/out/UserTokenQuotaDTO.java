package com.lofo.serenia.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Snapshot of current quota configuration and remaining credits for a user.
 */
public record UserTokenQuotaDTO(
    UUID id,
    UUID userId,
    Long inputTokensLimit,
    Long outputTokensLimit,
    Long totalTokensLimit,
    Long inputTokensUsed,
    Long outputTokensUsed,
    Long totalTokensUsed,
    Long remainingInputTokens,
    Long remainingOutputTokens,
    Long remainingTotalTokens,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
