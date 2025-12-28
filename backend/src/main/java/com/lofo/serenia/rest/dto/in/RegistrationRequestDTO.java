package com.lofo.serenia.rest.dto.in;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Captures the information required to create an account and trigger email verification.
 */
public record RegistrationRequestDTO(
        @NotBlank(message = "Last name is required") String lastName,
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must contain at least 6 characters") String password
) {
}
