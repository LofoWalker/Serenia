package com.lofo.serenia.rest.dto.out.admin;

public record EngagementStatsDTO(
        long activeUsers,
        double activationRate,
        double avgMessagesPerUser
) {}

