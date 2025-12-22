package com.lofo.serenia.service.user.activation;

import com.lofo.serenia.exception.exceptions.InvalidTokenException;
import com.lofo.serenia.persistence.entity.user.BaseToken;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.BaseTokenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for managing activation tokens.
 * Handles token generation, validation, and cleanup.
 */
@Slf4j
@ApplicationScoped
public class ActivationTokenService {

    private static final int ACTIVATION_TOKEN_EXPIRATION_MINUTES = 1440; // 24 hours
    private static final String ERROR_INVALID_TOKEN = "Invalid or expired token";

    private final BaseTokenRepository baseTokenRepository;

    @Inject
    public ActivationTokenService(BaseTokenRepository baseTokenRepository) {
        this.baseTokenRepository = baseTokenRepository;
    }

    /**
     * Generates and persists an activation token for a user.
     *
     * @param user the user to generate token for
     * @return the generated activation token string
     */
    @Transactional
    public String generateAndPersistActivationToken(User user) {
        String token = UUID.randomUUID().toString();
        BaseToken activationToken = BaseToken.builder()
                .token(token)
                .user(user)
                .expiryDate(calculateExpirationDate(ACTIVATION_TOKEN_EXPIRATION_MINUTES))
                .build();
        baseTokenRepository.persist(activationToken);
        log.debug("Activation token generated for user_id={}", user.getId());
        return token;
    }

    /**
     * Validates an activation token and returns the associated user.
     * Does NOT delete the token - caller must handle token consumption.
     *
     * @param token the activation token to validate
     * @return the user associated with the token
     * @throws InvalidTokenException if token is invalid or expired
     */
    public User validateToken(String token) {
        BaseToken activationToken = baseTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Token validation failed: invalid token");
                    return new InvalidTokenException(ERROR_INVALID_TOKEN);
                });

        if (activationToken.isExpired()) {
            log.warn("Token validation failed for user_id={}: token expired", activationToken.getUser().getId());
            throw new InvalidTokenException(ERROR_INVALID_TOKEN);
        }

        return activationToken.getUser();
    }

    /**
     * Deletes/consumes an activation token after successful activation.
     *
     * @param token the token to consume
     */
    @Transactional
    public void consumeToken(String token) {
        baseTokenRepository.deleteByToken(token);
        log.debug("Activation token consumed");
    }

    /**
     * Calculates the expiration date based on the specified number of minutes from now.
     *
     * @param expirationMinutes the number of minutes until expiration
     * @return the expiration instant
     */
    private Instant calculateExpirationDate(long expirationMinutes) {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }
}

