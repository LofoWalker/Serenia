package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.exception.AuthenticationFailedException;
import com.lofo.serenia.exception.UnactivatedAccountException;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import com.lofo.serenia.service.auth.AuthService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("AuthService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class AuthServiceTest {

    @Inject
    AuthService authService;

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
    @DisplayName("should_persist_user_with_hashed_password_and_role")
    void registerShouldPersistUserWithHashedPasswordAndRole() {
        String email = uniqueEmail();
        String rawPassword = "StrongPassword123!";
        RegistrationRequestDTO dto = new RegistrationRequestDTO("Doe", "John", email, rawPassword);

        UserResponseDTO created = authService.register(dto);

        assertNotNull(created.id());
        assertEquals(email, created.email());
        assertEquals(1, created.roles().size());

        User persisted = userRepository.find("email", email).firstResult();
        assertNotNull(persisted);
        assertEquals(email, persisted.getEmail());
        assertFalse(persisted.isAccountActivated());
        assertNotNull(persisted.getActivationToken());
        assertNotNull(persisted.getTokenExpirationDate());
    }

    @Test
    @DisplayName("should_fail_when_email_already_exists")
    void registerShouldFailWhenEmailAlreadyExists() {
        String email = uniqueEmail();
        RegistrationRequestDTO dto = new RegistrationRequestDTO("Doe", "Jane", email, "ComplexPassword321!");
        authService.register(dto);

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> authService.register(dto));
        assertEquals(Response.Status.CONFLICT.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("should_fail_when_login_account_not_activated")
    void loginShouldFailWhenAccountNotActivated() {
        String email = uniqueEmail();
        String password = "ValidPassword456!";
        authService.register(new RegistrationRequestDTO("Wayne", "Bruce", email, password));

        WebApplicationException exception = assertThrows(UnactivatedAccountException.class,
                () -> authService.login(new LoginRequestDTO(email, password)));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
        assertTrue(exception.getMessage().contains("activer votre compte"));
    }

    @Test
    @DisplayName("should_return_user_when_credentials_valid_and_activated")
    void loginShouldReturnUserWhenCredentialsValidAndActivated() {
        String email = uniqueEmail();
        String password = "ValidPassword456!";
        authService.register(new RegistrationRequestDTO("Wayne", "Bruce", email, password));

        User user = userRepository.find("email", email).firstResult();
        user.setAccountActivated(true);
        user.setActivationToken(null);
        user.setTokenExpirationDate(null);
        userRepository.persist(user);

        UserResponseDTO logged = authService.login(new LoginRequestDTO(email, password));

        assertEquals(email, logged.email());
    }

    @Test
    @DisplayName("should_fail_when_password_invalid")
    void loginShouldFailWhenPasswordInvalid() {
        String email = uniqueEmail();
        authService.register(new RegistrationRequestDTO("Kent", "Clark", email, "CorrectPassword789!"));

        User user = userRepository.find("email", email).firstResult();
        user.setAccountActivated(true);
        user.setActivationToken(null);
        user.setTokenExpirationDate(null);
        userRepository.persist(user);

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequestDTO(email, "WrongPassword")));
    }

    @Test
    @DisplayName("should_fail_when_email_unknown")
    void loginShouldFailWhenEmailUnknown() {
        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequestDTO(uniqueEmail(), "AnyPassword123!")));
    }

    @Nested
    @DisplayName("User registration limit")
    @Tag("unit")
    class UserRegistrationLimitTests {

        @Test
        @DisplayName("should_fail_when_max_users_limit_reached")
        void registerShouldFailWhenMaxUsersLimitReached() {
            long userCount = userRepository.count();
            long maxUsersLimit = 2L;

            for (long i = userCount; i < maxUsersLimit; i++) {
                String email = "user-" + i + "@example.com";
                authService.register(new RegistrationRequestDTO("Last-" + i, "First-" + i, email, "Password123!"));
            }

            long currentCount = userRepository.count();
            assertEquals(maxUsersLimit, currentCount);

            String emailBeyondLimit = "beyond-limit@example.com";
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> authService.register(new RegistrationRequestDTO("Beyond", "Limit", emailBeyondLimit, "Password123!")));

            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), exception.getResponse().getStatus());
            assertTrue(exception.getMessage().contains("Registration closed"));
        }

        @Test
        @DisplayName("should_succeed_when_under_max_users_limit")
        void registerShouldSucceedWhenUnderMaxUsersLimit() {
            long currentCount = userRepository.count();
            assertTrue(currentCount < 200L);

            String email = uniqueEmail();
            UserResponseDTO created = authService.register(new RegistrationRequestDTO("Under", "Limit", email, "Password123!"));

            assertNotNull(created.id());
            assertEquals(email, created.email());
            assertEquals(currentCount + 1, userRepository.count());
        }
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
