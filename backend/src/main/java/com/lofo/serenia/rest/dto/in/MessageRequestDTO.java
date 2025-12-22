package com.lofo.serenia.rest.dto.in;

import jakarta.validation.constraints.NotBlank;

/**
 * Single user utterance posted to the conversation resource.
 */
public record MessageRequestDTO(@NotBlank String content) {}