package com.lofo.serenia.rest.dto.out;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryDTO(
    UUID id,
    String name,
    Instant lastActivityAt
) {}

