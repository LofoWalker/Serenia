package com.lofo.serenia.util;

import io.smallrye.jwt.build.Jwt;

import java.time.Duration;
import java.util.UUID;

/**
 * Utility class for generating JWT tokens in integration tests.
 * Generates tokens with the same issuer and expiration as the production code.
 */
public class JwtTestTokenGenerator {

    private static final String DEFAULT_ISSUER = "serenia";
    private static final long DEFAULT_EXPIRATION_SECONDS = 3600;

    /**
     * Generates a test JWT token for a user with the specified email.
     *
     * @param email  The user's email address
     * @param userId The user's ID as UUID
     * @param role   The user's role
     * @return A valid JWT token string
     */
    public static String generateToken(String email, UUID userId, String role) {
        return generateToken(email, userId, role, DEFAULT_ISSUER, DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * Generates a test JWT token with custom issuer and expiration.
     *
     * @param email             The user's email address
     * @param userId            The user's ID as UUID
     * @param role              The user's role
     * @param issuer            The JWT issuer
     * @param expirationSeconds Token expiration time in seconds
     * @return A valid JWT token string
     */
    public static String generateToken(String email, UUID userId, String role, String issuer, long expirationSeconds) {
        return Jwt.issuer(issuer)
                .upn(email)
                .subject(userId.toString())
                .groups(role)
                .expiresIn(Duration.ofSeconds(expirationSeconds))
                .sign();
    }
}

