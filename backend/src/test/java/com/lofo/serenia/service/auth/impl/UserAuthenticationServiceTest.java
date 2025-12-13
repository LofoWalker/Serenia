package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.UnactivatedAccountException;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import com.lofo.serenia.service.auth.UserAuthenticationService;
import com.lofo.serenia.service.auth.UserRegistrationService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("UserAuthenticationService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class UserAuthenticationServiceTest {

    @Inject
    UserAuthenticationService userAuthenticationService;

    @Inject
    UserRegistrationService userRegistrationService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserTokenQuotaRepository userTokenQuotaRepository;

    @Inject
    UserTokenUsageRepository userTokenUsageRepository;

    @Inject
    RoleRepository roleRepository;

    @BeforeEach
    @Transactional
    void resetDatabase() {
        userTokenUsageRepository.deleteAll();
        userTokenQuotaRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.persist(Role.builder().name("USER").build());
    }

    @Test
    @DisplayName("should_fail_when_login_account_not_activated")
    void loginShouldFailWhenAccountNotActivated() {
        String email = uniqueEmail();
        String password = "ValidPassword456!";
        userRegistrationService.register(new RegistrationRequestDTO("Wayne", "Bruce", email, password));

        WebApplicationException exception = assertThrows(UnactivatedAccountException.class,
                () -> userAuthenticationService.login(new LoginRequestDTO(email, password)));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
        assertTrue(exception.getMessage().contains("activer votre compte"));
    }

    @Test
    @DisplayName("should_return_user_when_credentials_valid_and_activated")
    void loginShouldReturnUserWhenCredentialsValidAndActivated() {
        String email = uniqueEmail();
        String password = "ValidPassword456!";
        userRegistrationService.register(new RegistrationRequestDTO("Wayne", "Bruce", email, password));

        User user = userRepository.find("email", email).firstResult();
        user.setAccountActivated(true);
        userRepository.persist(user);

        UserResponseDTO logged = userAuthenticationService.login(new LoginRequestDTO(email, password));

        assertEquals(email, logged.email());
    }

    @Test
    @DisplayName("should_fail_when_password_invalid")
    void loginShouldFailWhenPasswordInvalid() {
        String email = uniqueEmail();
        userRegistrationService.register(new RegistrationRequestDTO("Kent", "Clark", email, "CorrectPassword789!"));

        User user = userRepository.find("email", email).firstResult();
        user.setAccountActivated(true);
        userRepository.persist(user);

        assertThrows(AuthenticationFailedException.class,
                () -> userAuthenticationService.login(new LoginRequestDTO(email, "WrongPassword")));
    }

    @Test
    @DisplayName("should_fail_when_email_unknown")
    void loginShouldFailWhenEmailUnknown() {
        assertThrows(AuthenticationFailedException.class,
                () -> userAuthenticationService.login(new LoginRequestDTO(uniqueEmail(), "AnyPassword123!")));
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
