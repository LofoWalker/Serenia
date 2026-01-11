package com.lofo.serenia.rest.dto.out.admin;

public record UserStatsDTO(
        long totalUsers,
        long activatedUsers,
        long freeUsers,
        long plusUsers,
        long maxUsers,
        long newUsersLast7Days,
        long newUsersLast30Days
) {}

