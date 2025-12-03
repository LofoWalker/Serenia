package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import com.lofo.serenia.service.auth.UserRegistrationService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("UserRegistrationService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class UserRegistrationServiceTest {

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
    @DisplayName("should_persist_user_with_hashed_password_and_role")
    void registerShouldPersistUserWithHashedPasswordAndRole() {
        String email = uniqueEmail();
        String rawPassword = "StrongPassword123!";
        RegistrationRequestDTO dto = new RegistrationRequestDTO("Doe", "John", email, rawPassword);

        UserResponseDTO created = userRegistrationService.register(dto);

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
        userRegistrationService.register(dto);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> userRegistrationService.register(dto));
        assertEquals(Response.Status.CONFLICT.getStatusCode(), exception.getResponse().getStatus());
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
                userRegistrationService
                        .register(new RegistrationRequestDTO("Last-" + i, "First-" + i, email, "Password123!"));
            }

            long currentCount = userRepository.count();
            assertEquals(maxUsersLimit, currentCount);

            String emailBeyondLimit = "beyond-limit@example.com";
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> userRegistrationService
                            .register(new RegistrationRequestDTO("Beyond", "Limit", emailBeyondLimit, "Password123!")));

            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), exception.getResponse().getStatus());
            assertTrue(exception.getMessage().contains("Registration closed"));
        }

        @Test
        @DisplayName("should_succeed_when_under_max_users_limit")
        void registerShouldSucceedWhenUnderMaxUsersLimit() {
            long currentCount = userRepository.count();
            assertTrue(currentCount < 200L);

            String email = uniqueEmail();
            UserResponseDTO created = userRegistrationService
                    .register(new RegistrationRequestDTO("Under", "Limit", email, "Password123!"));

            assertNotNull(created.id());
            assertEquals(email, created.email());
            assertEquals(currentCount + 1, userRepository.count());
        }
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
