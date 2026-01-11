package com.lofo.serenia.rest.dto.out.admin;

public record MessageStatsDTO(
        long totalUserMessages,
        long messagesToday,
        long messagesLast7Days,
        long messagesLast30Days
) {}

