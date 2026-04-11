package com.lofo.serenia.rest.util;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

/**
 * Utility for extracting the authenticated user ID from request context.
 * Prefers the JWT subject claim; falls back to the SecurityIdentity principal name.
 */
public final class AuthUtils {

    private AuthUtils() {}

    /**
     * Returns the UUID of the authenticated user.
     * Falls back to {@code securityIdentity} principal when the JWT subject is absent,
     * which can happen with alternative authentication mechanisms.
     */
    public static UUID getAuthenticatedUserId(JsonWebToken jwt, SecurityIdentity securityIdentity) {
        if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
            return UUID.fromString(jwt.getSubject());
        }
        return UUID.fromString(securityIdentity.getPrincipal().getName());
    }
}

