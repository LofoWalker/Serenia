package com.lofo.serenia.service.user;

import com.lofo.serenia.exception.exceptions.InvalidTokenException;
import com.lofo.serenia.persistence.entity.user.BaseToken;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.BaseTokenRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.service.mail.EmailTemplateProvider;
import com.lofo.serenia.service.mail.MailSender;
import com.lofo.serenia.service.user.shared.UserFinder;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for password recovery and reset operations.
 * Handles password reset token generation, validation, and password changes.
 */
@Slf4j
@ApplicationScoped
public class PasswordResetService {

    private static final int PASSWORD_TOKEN_EXPIRATION_MINUTES = 15;
    private static final String ERROR_INVALID_TOKEN = "Invalid or expired token";

    private final BaseTokenRepository baseTokenRepository;
    private final UserRepository userRepository;
    private final EmailTemplateProvider emailTemplateProvider;
    private final MailSender mailSender;
    private final UserFinder userFinder;
    private final String frontendUrl;

    @Inject
    public PasswordResetService(BaseTokenRepository baseTokenRepository, UserRepository userRepository,
                                EmailTemplateProvider emailTemplateProvider, MailSender mailSender,
                                UserFinder userFinder,
                                @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:4200")
                                String frontendUrl) {
        this.baseTokenRepository = baseTokenRepository;
        this.userRepository = userRepository;
        this.emailTemplateProvider = emailTemplateProvider;
        this.mailSender = mailSender;
        this.userFinder = userFinder;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Initiates password reset by generating token and sending email.
     * Does not indicate whether the email exists to prevent account enumeration attacks.
     * Token creation is transactional, email sending is not.
     *
     * @param email the email address for which to request a password reset
     */
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email={}", email);

        Optional<PasswordResetData> resetData = createPasswordResetToken(email);

        resetData.ifPresent(data -> sendPasswordResetEmailSafely(data.user(), data.resetLink()));
    }

    /**
     * Creates password reset token in a transaction.
     *
     * @param email the email address
     * @return optional containing reset data if user exists
     */
    @Transactional
    Optional<PasswordResetData> createPasswordResetToken(String email) {
        Optional<User> userOpt = userFinder.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for non-existent email={} - returning silently", email);
            return Optional.empty();
        }

        User user = userOpt.get();
        baseTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiryDate = calculateExpirationDate();

        BaseToken resetToken = BaseToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .build();

        baseTokenRepository.persist(resetToken);

        String resetLink = buildPasswordResetLink(token);
        log.info("Password reset token generated for user={}", user.getEmail());

        return Optional.of(new PasswordResetData(user, resetLink));
    }

    /**
     * Sends password reset email without affecting the transaction.
     *
     * @param user the user to send email to
     * @param resetLink the reset link
     */
    private void sendPasswordResetEmailSafely(User user, String resetLink) {
        try {
            sendPasswordResetEmail(user, resetLink);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Internal record for password reset data.
     */
    record PasswordResetData(User user, String resetLink) {}

    /**
     * Resets password with token validation.
     * Validates the token existence and expiration before allowing password change.
     *
     * @param token the password reset token
     * @param newPassword the new password to set
     * @throws InvalidTokenException if the token is invalid or expired
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        BaseToken resetToken = baseTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Password reset failed: invalid token");
                    return new InvalidTokenException(ERROR_INVALID_TOKEN);
                });

        if (resetToken.isExpired()) {
            log.warn("Password reset failed: token expired for user={}", resetToken.getUser().getEmail());
            baseTokenRepository.delete(resetToken);
            throw new InvalidTokenException(ERROR_INVALID_TOKEN);
        }

        User user = resetToken.getUser();
        String hashedPassword = BcryptUtil.bcryptHash(newPassword);
        user.setPassword(hashedPassword);
        userRepository.persist(user);
        baseTokenRepository.delete(resetToken);

        log.info("Password successfully reset for user={}", user.getEmail());
    }

    /**
     * Sends password reset email to the user.
     *
     * @param user the user to send reset email to
     * @param resetLink the password reset link
     */
    private void sendPasswordResetEmail(User user, String resetLink) {
        log.info("Sending password reset email to {}", user.getEmail());

        String subject = emailTemplateProvider.getPasswordResetEmailSubject();
        String htmlContent = emailTemplateProvider.getPasswordResetEmailBody(user.getFirstName(), resetLink);

        mailSender.sendHtml(user.getEmail(), subject, htmlContent);
    }

    /**
     * Builds the password reset link.
     *
     * @param token the password reset token
     * @return the complete reset link
     */
    private String buildPasswordResetLink(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }

    /**
     * Calculates the expiration date for the password reset token.
     *
     * @return the expiration instant
     */
    private Instant calculateExpirationDate() {
        return Instant.now().plus(PASSWORD_TOKEN_EXPIRATION_MINUTES, ChronoUnit.MINUTES);
    }
}

