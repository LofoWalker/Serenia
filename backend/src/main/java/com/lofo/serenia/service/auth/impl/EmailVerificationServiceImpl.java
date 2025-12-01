package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.EmailVerificationService;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@ApplicationScoped
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger LOG = Logger.getLogger(EmailVerificationServiceImpl.class);

    private final UserRepository userRepository;
    private final Mailer mailer;

    @Inject
    public EmailVerificationServiceImpl(UserRepository userRepository, Mailer mailer) {
        this.userRepository = userRepository;
        this.mailer = mailer;
    }

    @Override
    @Transactional
    public void sendActivationEmail(User user, String activationLink) {
        LOG.infof("Sending activation email to %s", user.getEmail());

        String subject = "Serenia - Activation de votre compte";
        String htmlContent = buildActivationEmailContent(user.getFirstName(), activationLink);

        Mail mail = Mail.withHtml(user.getEmail(), subject, htmlContent);
        mailer.send(mail);

        LOG.infof("Activation email sent successfully to %s", user.getEmail());
    }

    @Override
    @Transactional
    public void activateAccount(String token) {
        LOG.infof("Attempting to activate account with token=%s", maskToken(token));

        User user = userRepository.find("activationToken", token)
                .firstResultOptional()
                .orElseThrow(() -> {
                    LOG.warnf("Activation failed: invalid token");
                    return new WebApplicationException("Jeton invalide", Response.Status.BAD_REQUEST);
                });

        if (user.getTokenExpirationDate() != null && Instant.now().isAfter(user.getTokenExpirationDate())) {
            LOG.warnf("Activation failed for email=%s: token expired", user.getEmail());
            throw new WebApplicationException("Jeton expiré", Response.Status.BAD_REQUEST);
        }

        user.setAccountActivated(true);
        user.setActivationToken(null);
        user.setTokenExpirationDate(null);
        userRepository.persist(user);

        LOG.infof("Account successfully activated for email=%s", user.getEmail());
    }

    private String buildActivationEmailContent(String firstName, String activationLink) {
        return String.format(
                "<html>"
                        + "<body style=\"font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;\">"
                        + "<div style=\"background-color: white; padding: 30px; border-radius: 8px; max-width: 600px; margin: 0 auto;\">"
                        + "<h2 style=\"color: #333; margin-bottom: 20px;\">Bienvenue sur Serenia, %s!</h2>"
                        + "<p style=\"color: #666; line-height: 1.6;\">"
                        + "Merci de vous être inscrit(e). Pour activer votre compte, veuillez cliquer sur le lien ci-dessous:"
                        + "</p>"
                        + "<div style=\"margin: 30px 0; text-align: center;\">"
                        + "<a href=\"%s\" style=\"background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; display: inline-block;\">"
                        + "Activer mon compte"
                        + "</a>"
                        + "</div>"
                        + "<p style=\"color: #999; font-size: 12px; margin-top: 30px; border-top: 1px solid #eee; padding-top: 20px;\">"
                        + "Ce lien expirera dans 24 heures. Si vous n'avez pas demandé cette inscription, veuillez ignorer cet email."
                        + "</p>"
                        + "</div>"
                        + "</body>"
                        + "</html>",
                firstName, activationLink);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    public static String generateActivationToken() {
        return UUID.randomUUID().toString();
    }

    public static Instant calculateExpirationDate(long expirationMinutes) {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }
}

