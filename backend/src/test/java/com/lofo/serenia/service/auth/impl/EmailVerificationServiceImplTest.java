package com.lofo.serenia.service.auth.impl;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import com.lofo.serenia.service.auth.EmailVerificationService;
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

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@DisplayName("EmailVerificationService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class EmailVerificationServiceImplTest {

    @Inject
    EmailVerificationService emailVerificationService;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    UserTokenQuotaRepository userTokenQuotaRepository;

    @Inject
    UserTokenUsageRepository userTokenUsageRepository;

    private Role testRole;

    @BeforeEach
    @Transactional
    void setup() {
        userTokenUsageRepository.deleteAll();
        userTokenQuotaRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        testRole = Role.builder().name("USER").build();
        roleRepository.persist(testRole);
    }

    @Test
    @DisplayName("should_activate_account_when_token_valid_and_not_expired")
    @Transactional
    void should_activate_account_when_token_valid_and_not_expired() {
        String token = EmailVerificationServiceImpl.generateActivationToken();
        User user = User.builder()
                .email("test@example.com")
                .firstName("Jean")
                .lastName("Dupont")
                .password("hashedPassword")
                .accountActivated(false)
                .activationToken(token)
                .tokenExpirationDate(EmailVerificationServiceImpl.calculateExpirationDate(1440))
                .roles(Set.of(testRole))
                .build();
        userRepository.persist(user);

        emailVerificationService.activateAccount(token);

        User activated = userRepository.find("email", "test@example.com").firstResult();
        assertTrue(activated.isAccountActivated());
        assertNull(activated.getActivationToken());
        assertNull(activated.getTokenExpirationDate());
    }

    @Test
    @DisplayName("should_fail_when_activation_token_invalid")
    void should_fail_when_activation_token_invalid() {
        String invalidToken = "invalid-token-xyz";

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> emailVerificationService.activateAccount(invalidToken));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("should_fail_when_activation_token_expired")
    @Transactional
    void should_fail_when_activation_token_expired() {
        String token = EmailVerificationServiceImpl.generateActivationToken();
        Instant expiredDate = Instant.now().minusSeconds(3600);
        User user = User.builder()
                .email("expired@example.com")
                .firstName("Jean")
                .lastName("Dupont")
                .password("hashedPassword")
                .accountActivated(false)
                .activationToken(token)
                .tokenExpirationDate(expiredDate)
                .roles(Set.of(testRole))
                .build();
        userRepository.persist(user);

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> emailVerificationService.activateAccount(token));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    @DisplayName("should_generate_token_with_uuid_format")
    void should_generate_token_with_uuid_format() {
        String token = EmailVerificationServiceImpl.generateActivationToken();

        assertThat(token).isNotEmpty();
        assertDoesNotThrow(() -> UUID.fromString(token));
    }

    @Test
    @DisplayName("should_calculate_expiration_date_correctly")
    void should_calculate_expiration_date_correctly() {
        long expirationMinutes = 1440;
        Instant beforeCalculation = Instant.now();

        Instant expirationDate = EmailVerificationServiceImpl.calculateExpirationDate(expirationMinutes);

        Instant afterCalculation = Instant.now();
        assertThat(expirationDate).isAfter(beforeCalculation).isAfter(afterCalculation);
        assertThat(expirationDate.toEpochMilli() - beforeCalculation.toEpochMilli())
                .isGreaterThan(expirationMinutes * 60 * 1000 - 1000)
                .isLessThan(expirationMinutes * 60 * 1000 + 1000);
    }
}


