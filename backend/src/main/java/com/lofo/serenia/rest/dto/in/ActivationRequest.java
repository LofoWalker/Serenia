package com.lofo.serenia.rest.dto.in;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for account activation.
 */
public record ActivationRequest(
    @NotBlank(message = "Token is required")
    String token
) {
}

