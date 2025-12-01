package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.exception.AuthenticationFailedException;
import com.lofo.serenia.exception.UnactivatedAccountException;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.repository.*;
import com.lofo.serenia.service.auth.AuthService;
import com.lofo.serenia.service.auth.EmailVerificationService;
import com.lofo.serenia.service.token.TokenUsageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class);
    private static final String DEFAULT_ROLE_NAME = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserMapper userMapper;
    private final TokenUsageService tokenUsageService;
    private final SereniaConfig sereniaConfig;
    private final UserTokenQuotaRepository userTokenQuotaRepository;
    private final UserTokenUsageRepository userTokenUsageRepository;
    private final EmailVerificationService emailVerificationService;

    @Inject
    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository,
                           UserMapper userMapper,
                           TokenUsageService tokenUsageService, SereniaConfig sereniaConfig, UserTokenQuotaRepository userTokenQuotaRepository, UserTokenUsageRepository userTokenUsageRepository, EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userMapper = userMapper;
        this.tokenUsageService = tokenUsageService;
        this.sereniaConfig = sereniaConfig;
        this.userTokenQuotaRepository = userTokenQuotaRepository;
        this.userTokenUsageRepository = userTokenUsageRepository;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    @Override
    public UserResponseDTO register(RegistrationRequestDTO dto) {
        LOG.infof("Registering user with email=%s", dto.email());

        long userCount = userRepository.count();
        if (userCount >= sereniaConfig.maxUsers()) {
            LOG.warnf("Registration failed, max users limit (%d) reached", sereniaConfig.maxUsers());
            throw new WebApplicationException("Registration closed: user limit reached", Response.Status.SERVICE_UNAVAILABLE);
        }

        boolean exists = userRepository.find("email", dto.email()).firstResultOptional().isPresent();
        if (exists) {
            LOG.warnf("Registration failed, email already exists: %s", dto.email());
            throw new WebApplicationException("Email already exists", Response.Status.CONFLICT);
        }

        Role defaultRole = roleRepository.find("name", DEFAULT_ROLE_NAME).firstResultOptional()
                .orElseThrow(() -> new WebApplicationException("Default role USER not found", Response.Status.INTERNAL_SERVER_ERROR));

        String activationToken = EmailVerificationServiceImpl.generateActivationToken();
        long tokenExpirationMinutes = 1440;

        User user = User.builder()
                .email(dto.email())
                .password(BCrypt.hashpw(dto.password(), BCrypt.gensalt()))
                .lastName(dto.lastName())
                .firstName(dto.firstName())
                .accountActivated(false)
                .activationToken(activationToken)
                .tokenExpirationDate(EmailVerificationServiceImpl.calculateExpirationDate(tokenExpirationMinutes))
                .roles(Set.of(defaultRole))
                .build();

        userRepository.persist(user);
        tokenUsageService.initializeUserTokenQuota(user);

        String activationLink = buildActivationLink(activationToken);
        emailVerificationService.sendActivationEmail(user, activationLink);

        LOG.infof("User %s successfully registered, activation email sent", user.getEmail());
        return userMapper.toView(user);
    }

    @Override
    public UserResponseDTO login(LoginRequestDTO dto) {
        LOG.infof("Login attempt for email=%s", dto.email());
        User user = userRepository.find("email", dto.email())
                .firstResultOptional()
                .orElseThrow(() -> invalidCredentials(dto.email()));

        if (!BCrypt.checkpw(dto.password(), user.getPassword())) {
            throw invalidCredentials(dto.email());
        }

        if (!user.isAccountActivated()) {
            LOG.warnf("Login failed for email=%s: account not activated", dto.email());
            throw new UnactivatedAccountException(UnactivatedAccountException.USER_MESSAGE);
        }

        LOG.infof("Login success for email=%s", dto.email());
        return userMapper.toView(user);
    }

    @Override
    public UserResponseDTO getByEmail(String email) {
        LOG.debugf("Fetching user by email=%s", email);
        return userRepository.find("email", email).firstResultOptional()
                .map(userMapper::toView)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));
    }

    @Override
    @Transactional
    public void deleteAccount(String email) {
        LOG.infof("Deleting account for email=%s", email);
        User user = userRepository.find("email", email).firstResultOptional()
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));
        UUID userId = user.getId();

        userTokenUsageRepository.deleteByUserId(userId);
        userTokenQuotaRepository.deleteByUserId(userId);
        messageRepository.deleteByUserId(userId);
        conversationRepository.deleteByUserId(userId);

        long deletedUsers = userRepository.delete("id", userId);
        if (deletedUsers != 1) {
            LOG.errorf("Unexpected delete result for user %s: %d rows", email, deletedUsers);
            throw new WebApplicationException("Unable to delete account", Response.Status.INTERNAL_SERVER_ERROR);
        }

        LOG.infof("User %s and related data deleted", email);
    }

    private String buildActivationLink(String activationToken) {
        return sereniaConfig.url() + "/api/auth/activate?token=" + activationToken;
    }

    private AuthenticationFailedException invalidCredentials(String email) {
        LOG.warnf("Login failed for email=%s", email);
        return new AuthenticationFailedException("Invalid credentials");
    }
}
