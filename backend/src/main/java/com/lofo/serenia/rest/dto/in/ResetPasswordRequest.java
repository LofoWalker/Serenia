package com.lofo.serenia.rest.dto.in;

import com.lofo.serenia.validation.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for resetting password with a valid token.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "New password is required")
        @ValidPassword
        String newPassword
) {
}

