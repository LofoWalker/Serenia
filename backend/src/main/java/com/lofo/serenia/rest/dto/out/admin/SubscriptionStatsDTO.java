package com.lofo.serenia.rest.dto.out.admin;

public record SubscriptionStatsDTO(
        long totalTokensConsumed,
        int estimatedRevenueCents,
        String currency
) {}

