package com.lofo.serenia.rest.dto.in;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record MessageRequestDTO(@NotBlank String content, @Nullable UUID conversationId) {

    public MessageRequestDTO(String content) {
        this(content, null);
    }
}
