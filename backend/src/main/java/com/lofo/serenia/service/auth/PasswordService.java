package com.lofo.serenia.service.auth;

/**
 * Handles password reset flow including token generation, validation and password update.
 */
public interface PasswordService {

    /**
     * Initiates password reset by generating a token and sending a reset email.
     * For security, this method returns silently if the user does not exist (prevents user enumeration).
     *
     * @param email the user's email address
     */
    void requestReset(String email);

    /**
     * Validates the reset token and updates the user's password.
     *
     * @param token       the password reset token
     * @param newPassword the new password to set
     * @throws com.lofo.serenia.exception.exceptions.InvalidResetTokenException if token is invalid or expired
     */
    void resetPassword(String token, String newPassword);
}

