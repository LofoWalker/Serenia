package com.lofo.serenia.rest.dto.out.admin;

public record DashboardDTO(
        UserStatsDTO users,
        MessageStatsDTO messages,
        EngagementStatsDTO engagement,
        SubscriptionStatsDTO subscriptions
) {}

