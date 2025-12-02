package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.EmailVerificationService;
import com.lofo.serenia.service.auth.UserRegistrationService;
import com.lofo.serenia.service.token.TokenUsageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.jboss.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Set;

@AllArgsConstructor
@ApplicationScoped
public class UserRegistrationServiceImpl implements UserRegistrationService {

    private static final Logger LOG = Logger.getLogger(UserRegistrationServiceImpl.class);
    private static final String DEFAULT_ROLE_NAME = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final TokenUsageService tokenUsageService;
    private final SereniaConfig sereniaConfig;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    @Override
    public UserResponseDTO register(RegistrationRequestDTO dto) {
        LOG.infof("Registering user with email=%s", dto.email());

        enforceRegistrationIsOpen();
        enforceUserAvailability(dto);

        Role defaultRole = getDefaultRole();
        String activationToken = EmailVerificationServiceImpl.generateActivationToken();

        User user = buildUser(dto, activationToken, defaultRole);

        userRepository.persist(user);
        tokenUsageService.initializeUserTokenQuota(user);

        String activationLink = buildActivationLink(activationToken);
        emailVerificationService.sendActivationEmail(user, activationLink);

        LOG.infof("User %s successfully registered, activation email sent", user.getEmail());
        return userMapper.toView(user);
    }

    private static User buildUser(RegistrationRequestDTO dto, String activationToken, Role defaultRole) {
        return User.builder()
                .email(dto.email())
                .password(BCrypt.hashpw(dto.password(), BCrypt.gensalt()))
                .lastName(dto.lastName())
                .firstName(dto.firstName())
                .accountActivated(false)
                .activationToken(activationToken)
                .tokenExpirationDate(EmailVerificationServiceImpl.calculateExpirationDate(1440))
                .roles(Set.of(defaultRole))
                .build();
    }

    private Role getDefaultRole() {
        return roleRepository.find("name", DEFAULT_ROLE_NAME).firstResultOptional()
                .orElseThrow(() -> new WebApplicationException("Default role USER not found",
                        Response.Status.INTERNAL_SERVER_ERROR));
    }

    private void enforceUserAvailability(RegistrationRequestDTO dto) {
        boolean exists = userRepository.find("email", dto.email()).firstResultOptional().isPresent();
        if (exists) {
            LOG.warnf("Registration failed, email already exists: %s", dto.email());
            throw new WebApplicationException("Email already exists", Response.Status.CONFLICT);
        }
    }

    private void enforceRegistrationIsOpen() {
        long userCount = userRepository.count();

        if (userCount >= sereniaConfig.maxUsers()) {
            LOG.warnf("Registration failed, max users limit (%d) reached", sereniaConfig.maxUsers());
            throw new WebApplicationException("Registration closed: user limit reached",
                    Response.Status.SERVICE_UNAVAILABLE);
        }
    }

    private String buildActivationLink(String activationToken) {
        return sereniaConfig.url() + "/api/auth/activate?token=" + activationToken;
    }
}
