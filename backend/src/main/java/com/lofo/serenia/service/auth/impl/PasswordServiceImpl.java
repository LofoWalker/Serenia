package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.domain.user.PasswordResetToken;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.exception.exceptions.InvalidResetTokenException;
import com.lofo.serenia.repository.PasswordResetTokenRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.PasswordService;
import com.lofo.serenia.service.notification.EmailTemplateProvider;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private static final Logger LOG = Logger.getLogger(PasswordServiceImpl.class);
    private static final int TOKEN_EXPIRATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Mailer mailer;
    private final EmailTemplateProvider emailTemplateProvider;

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:4200")
    String frontendUrl;

    @Override
    @Transactional
    public void requestReset(String email) {
        LOG.infof("Password reset requested for email=%s", email);

        Optional<User> userOpt = userRepository.find("email", email).firstResultOptional();

        if (userOpt.isEmpty()) {
            LOG.infof("Password reset requested for non-existent email=%s - returning silently", email);
            return;
        }

        User user = userOpt.get();

        passwordResetTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(TOKEN_EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .build();

        passwordResetTokenRepository.persist(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        sendPasswordResetEmail(user, resetLink);

        LOG.infof("Password reset token generated and email sent for user=%s", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        LOG.infof("Attempting to reset password with token=%s", maskToken(token));

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    LOG.warnf("Password reset failed: invalid token");
                    return new InvalidResetTokenException("Jeton invalide ou expiré");
                });

        if (resetToken.isExpired()) {
            LOG.warnf("Password reset failed: token expired for user=%s", resetToken.getUser().getEmail());
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidResetTokenException("Jeton invalide ou expiré");
        }

        User user = resetToken.getUser();
        String hashedPassword = BcryptUtil.bcryptHash(newPassword);
        user.setPassword(hashedPassword);
        userRepository.persist(user);

        passwordResetTokenRepository.delete(resetToken);

        LOG.infof("Password successfully reset for user=%s", user.getEmail());
    }

    private void sendPasswordResetEmail(User user, String resetLink) {
        LOG.infof("Sending password reset email to %s", user.getEmail());

        String subject = emailTemplateProvider.getPasswordResetEmailSubject();
        String htmlContent = emailTemplateProvider.getPasswordResetEmailBody(user.getFirstName(), resetLink);

        Mail mail = Mail.withHtml(user.getEmail(), subject, htmlContent);
        mailer.send(mail);

        LOG.infof("Password reset email sent successfully to %s", user.getEmail());
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}

