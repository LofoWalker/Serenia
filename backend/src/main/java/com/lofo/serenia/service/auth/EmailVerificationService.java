package com.lofo.serenia.service.auth;

import com.lofo.serenia.domain.user.User;

/**
 * Handles activation email generation, dispatch and token validation.
 */
public interface EmailVerificationService {

    /**
     * Sends an activation email containing the provided link.
     */
    void sendActivationEmail(User user, String activationLink);

    /**
     * Validates the token and activates the user account.
     */
    void activateAccount(String token);
}
