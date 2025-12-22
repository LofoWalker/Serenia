package com.lofo.serenia.rest.dto.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for any ExpiringToken.
 *
 * Provides a unified view of token information for REST responses.
 * Generic enough to represent both activation and password reset tokens.
 *
 * @param id the unique token identifier
 * @param type the type of token (e.g., "ACTIVATION", "PASSWORD_RESET")
 * @param userId the ID of the user this token belongs to
 * @param userEmail the email of the user this token belongs to
 * @param expiryDate when this token expires
 * @param isExpired whether this token is currently expired
 * @param remainingSeconds seconds until expiration (negative if expired)
 */
public record TokenDTO(
    UUID id,
    String type,
    UUID userId,
    String userEmail,
    Instant expiryDate,
    boolean isExpired,
    long remainingSeconds
) {
}

