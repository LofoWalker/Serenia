package com.lofo.serenia.rest.dto.in;

import jakarta.validation.constraints.NotBlank;

public record RenameConversationRequestDTO(@NotBlank String name) {}

