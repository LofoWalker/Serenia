package com.lofo.serenia.service.user.activation;

import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.service.mail.MailSender;
import com.lofo.serenia.service.mail.provider.EmailTemplateProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for account activation operations.
 * Handles the business logic of activating user accounts.
 */
@Slf4j
@ApplicationScoped
public class AccountActivationService {

    private final ActivationTokenService activationTokenService;
    private final UserRepository userRepository;
    private final EmailTemplateProvider emailTemplateProvider;
    private final MailSender mailSender;

    @Inject
    public AccountActivationService(ActivationTokenService activationTokenService,
                                     UserRepository userRepository,
                                     EmailTemplateProvider emailTemplateProvider,
                                     MailSender mailSender) {
        this.activationTokenService = activationTokenService;
        this.userRepository = userRepository;
        this.emailTemplateProvider = emailTemplateProvider;
        this.mailSender = mailSender;
    }

    /**
     * Activates a user account using the provided activation token.
     * Validates the token, marks the account as activated, and consumes the token.
     *
     * @param token the activation token
     * @throws com.lofo.serenia.exception.exceptions.InvalidTokenException if token is invalid or expired
     */
    @Transactional
    public void activateAccount(String token) {
        User user = activationTokenService.validateToken(token);

        user.setAccountActivated(true);
        userRepository.persist(user);

        activationTokenService.consumeToken(token);

        log.info("Account successfully activated for email={}", user.getEmail());
    }

    /**
     * Generates an activation token for a user.
     * Delegates to ActivationTokenService.
     *
     * @param user the user to generate token for
     * @return the generated activation token
     */
    public String generateAndPersistActivationToken(User user) {
        return activationTokenService.generateAndPersistActivationToken(user);
    }

    /**
     * Sends activation email to the user.
     *
     * @param user the user to send activation email to
     * @param activationLink the activation link to include in the email
     */
    public void sendActivationEmail(User user, String activationLink) {
        log.info("Sending activation email to {}", user.getEmail());

        String subject = emailTemplateProvider.getActivationEmailSubject();
        String htmlContent = emailTemplateProvider.getActivationEmailBody(user.getFirstName(), activationLink);

        mailSender.sendHtml(user.getEmail(), subject, htmlContent);
    }
}

