package com.lofo.serenia.rest.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Carries raw user credentials for the authentication endpoint.
 */
public record LoginRequestDTO(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password is required") String password
) {
}
