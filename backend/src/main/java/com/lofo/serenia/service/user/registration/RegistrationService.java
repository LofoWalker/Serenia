package com.lofo.serenia.service.user.registration;
import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.service.subscription.SubscriptionService;
import com.lofo.serenia.service.user.activation.AccountActivationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

@Slf4j
@ApplicationScoped
public class RegistrationService {

    private static final String ERROR_EMAIL_EXISTS = "Email already exists";
    private static final String ERROR_REGISTRATION_CLOSED = "Registration closed: maximum user limit reached";
    private final UserRepository userRepository;
    private final SereniaConfig sereniaConfig;
    private final AccountActivationService accountActivationService;
    private final SubscriptionService subscriptionService;

    @Inject
    public RegistrationService(UserRepository userRepository,
                               SereniaConfig sereniaConfig,
                               AccountActivationService accountActivationService,
                               SubscriptionService subscriptionService) {
        this.userRepository = userRepository;
        this.sereniaConfig = sereniaConfig;
        this.accountActivationService = accountActivationService;
        this.subscriptionService = subscriptionService;
    }

    public void register(RegistrationRequestDTO dto) {
        log.info("Registering user with email={}", dto.email());
        RegistrationResult result = createUserAndToken(dto);
        sendActivationEmailSafely(result.user(), result.activationLink());
        log.info("User {} successfully registered, activation email sent", dto.email());
    }

    @Transactional
    RegistrationResult createUserAndToken(RegistrationRequestDTO dto) {
        validateRegistrationOpen();
        validateEmailAvailability(dto.email());
        User user = createUser(dto);
        userRepository.persist(user);

        // Créer automatiquement une subscription FREE pour le nouvel utilisateur
        subscriptionService.createDefaultSubscription(user.getId());
        log.info("Created FREE subscription for new user {}", user.getId());

        String activationToken = accountActivationService.generateAndPersistActivationToken(user);
        String activationLink = buildActivationLink(activationToken);
        return new RegistrationResult(user, activationLink);
    }

    private void sendActivationEmailSafely(User user, String activationLink) {
        try {
            accountActivationService.sendActivationEmail(user, activationLink);
        } catch (Exception e) {
            log.error("Failed to send activation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildActivationLink(String activationToken) {
        return sereniaConfig.frontUrl() + "/activate?token=" + activationToken;
    }

    private User createUser(RegistrationRequestDTO dto) {
        return User.builder()
                .email(dto.email())
                .password(BCrypt.hashpw(dto.password(), BCrypt.gensalt()))
                .lastName(dto.lastName())
                .firstName(dto.firstName())
                .accountActivated(false)
                .build();
    }

    private void validateEmailAvailability(String email) {
        boolean exists = userRepository.find("email", email).firstResultOptional().isPresent();

        if (exists) {
            log.warn("Registration failed, email already exists: {}", email);
            throw SereniaException.conflict(ERROR_EMAIL_EXISTS);
        }
    }

    private void validateRegistrationOpen() {
        long userCount = userRepository.count();
        if (userCount >= sereniaConfig.maxUsers()) {
            log.warn("Registration failed, max users limit ({}) reached", sereniaConfig.maxUsers());
            throw SereniaException.conflict(ERROR_REGISTRATION_CLOSED);
        }
    }

    record RegistrationResult(User user, String activationLink) {}
}
