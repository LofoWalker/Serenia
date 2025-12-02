package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.EmailVerificationService;
import com.lofo.serenia.service.notification.EmailTemplateProvider;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger LOG = Logger.getLogger(EmailVerificationServiceImpl.class);

    private final UserRepository userRepository;
    private final Mailer mailer;
    private final EmailTemplateProvider emailTemplateProvider;

    @Override
    @Transactional
    public void sendActivationEmail(User user, String activationLink) {
        LOG.infof("Sending activation email to %s", user.getEmail());

        String subject = emailTemplateProvider.getActivationEmailSubject();
        String htmlContent = emailTemplateProvider.getActivationEmailBody(user.getFirstName(), activationLink);

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
