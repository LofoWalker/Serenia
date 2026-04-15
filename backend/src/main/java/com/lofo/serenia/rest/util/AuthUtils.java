package com.lofo.serenia.rest.util;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/**
 * Utility for extracting the authenticated user ID from the JWT subject claim.
 */
public final class AuthUtils {

    private AuthUtils() {}

    /**
     * Returns the UUID of the authenticated user from the JWT {@code sub} claim.
     *
     * @throws IllegalStateException if the JWT subject is missing, blank, or not a valid UUID
     */
    public static UUID getAuthenticatedUserId(JsonWebToken jwt, SecurityIdentity securityIdentity) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalStateException("Authenticated user ID is missing from JWT subject");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT subject is not a valid UUID: " + jwt.getSubject(), e);
        }
    }
}

