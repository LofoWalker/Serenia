package com.lofo.serenia.rest.dto.out.admin;

import java.time.Instant;
import java.util.UUID;

public record UserDetailDTO(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        String planType,
        boolean activated,
        Instant createdAt,
        long messageCount,
        int tokensUsedThisMonth,
        int messagesSentToday
) {}

