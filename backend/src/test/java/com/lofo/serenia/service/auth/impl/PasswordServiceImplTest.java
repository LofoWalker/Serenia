package com.lofo.serenia.service.auth.impl;
import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.PasswordResetToken;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.exception.exceptions.InvalidResetTokenException;
import com.lofo.serenia.repository.PasswordResetTokenRepository;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.PasswordService;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
@QuarkusTest
@DisplayName("PasswordService unit tests")
@Tag("unit")
@TestProfile(TestResourceProfile.class)
class PasswordServiceImplTest {
    @Inject
    PasswordService passwordService;
    @Inject
    UserRepository userRepository;
    @Inject
    RoleRepository roleRepository;
    @Inject
    PasswordResetTokenRepository passwordResetTokenRepository;
    private Role testRole;
    @BeforeEach
    @Transactional
    void setup() {
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        testRole = Role.builder().name("USER").build();
        roleRepository.persist(testRole);
    }
    @Test
    @DisplayName("should_generate_token_and_persist_when_user_exists")
    @Transactional
    void should_generate_token_and_persist_when_user_exists() {
        User user = createAndPersistUser("existing@example.com");
        passwordService.requestReset("existing@example.com");
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
                .find("user.id", user.getId())
                .firstResultOptional();
        assertThat(tokenOpt).isPresent();
        assertThat(tokenOpt.get().getToken()).isNotBlank();
        assertThat(tokenOpt.get().getExpiryDate()).isAfter(Instant.now());
    }
    @Test
    @DisplayName("should_return_silently_when_user_does_not_exist")
    void should_return_silently_when_user_does_not_exist() {
        assertDoesNotThrow(() -> passwordService.requestReset("nonexistent@example.com"));
        long tokenCount = passwordResetTokenRepository.count();
        assertThat(tokenCount).isZero();
    }
    @Test
    @DisplayName("should_delete_previous_tokens_when_requesting_new_reset")
    @Transactional
    void should_delete_previous_tokens_when_requesting_new_reset() {
        User user = createAndPersistUser("user@example.com");
        PasswordResetToken existingToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
        passwordResetTokenRepository.persist(existingToken);
        UUID existingTokenId = existingToken.getId();
        passwordService.requestReset("user@example.com");
        Optional<PasswordResetToken> oldToken = passwordResetTokenRepository
                .find("id", existingTokenId)
                .firstResultOptional();
        assertThat(oldToken).isEmpty();
        long tokenCount = passwordResetTokenRepository.find("user.id", user.getId()).count();
        assertThat(tokenCount).isEqualTo(1);
    }
    @Test
    @DisplayName("should_set_token_expiry_to_15_minutes")
    @Transactional
    void should_set_token_expiry_to_15_minutes() {
        User user = createAndPersistUser("timing@example.com");
        Instant beforeRequest = Instant.now();
        passwordService.requestReset("timing@example.com");
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
                .find("user.id", user.getId())
                .firstResultOptional();
        assertThat(tokenOpt).isPresent();
        Instant expiryDate = tokenOpt.get().getExpiryDate();
        Instant expectedMinExpiry = beforeRequest.plus(14, ChronoUnit.MINUTES);
        Instant expectedMaxExpiry = beforeRequest.plus(16, ChronoUnit.MINUTES);
        assertThat(expiryDate).isAfter(expectedMinExpiry).isBefore(expectedMaxExpiry);
    }
    @Test
    @DisplayName("should_update_password_when_token_valid")
    @Transactional
    void should_update_password_when_token_valid() {
        User user = createAndPersistUser("reset@example.com");
        String originalPassword = user.getPassword();
        String token = UUID.randomUUID().toString();
        createAndPersistToken(token, user, Instant.now().plus(15, ChronoUnit.MINUTES));
        passwordService.resetPassword(token, "newSecurePassword123");
        User updatedUser = userRepository.find("email", "reset@example.com").firstResult();
        assertThat(updatedUser.getPassword()).isNotEqualTo(originalPassword);
        assertThat(BcryptUtil.matches("newSecurePassword123", updatedUser.getPassword())).isTrue();
    }
    @Test
    @DisplayName("should_delete_token_after_successful_reset")
    @Transactional
    void should_delete_token_after_successful_reset() {
        User user = createAndPersistUser("delete@example.com");
        String token = UUID.randomUUID().toString();
        createAndPersistToken(token, user, Instant.now().plus(15, ChronoUnit.MINUTES));
        passwordService.resetPassword(token, "newPassword123");
        Optional<PasswordResetToken> deletedToken = passwordResetTokenRepository.findByToken(token);
        assertThat(deletedToken).isEmpty();
    }
    @Test
    @DisplayName("should_throw_exception_when_token_not_found")
    void should_throw_exception_when_token_not_found() {
        assertThatThrownBy(() -> passwordService.resetPassword("invalid-token", "newPassword"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessageContaining("Jeton invalide ou expiré");
    }
    @Test
    @DisplayName("should_throw_exception_when_token_expired")
    @Transactional
    void should_throw_exception_when_token_expired() {
        User user = createAndPersistUser("expired@example.com");
        String token = UUID.randomUUID().toString();
        createAndPersistToken(token, user, Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> passwordService.resetPassword(token, "newPassword"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessageContaining("Jeton invalide ou expiré");
    }
    @Test
    @DisplayName("should_delete_expired_token_when_reset_attempted")
    @Transactional
    void should_delete_expired_token_when_reset_attempted() {
        User user = createAndPersistUser("cleanup@example.com");
        String token = UUID.randomUUID().toString();
        createAndPersistToken(token, user, Instant.now().minus(1, ChronoUnit.MINUTES));
        try {
            passwordService.resetPassword(token, "newPassword");
        } catch (InvalidResetTokenException e) {
            // Expected exception - we are testing that the token gets deleted even on failure
        }
        Optional<PasswordResetToken> deletedToken = passwordResetTokenRepository.findByToken(token);
        assertThat(deletedToken).isEmpty();
    }
    @Test
    @DisplayName("should_hash_password_with_bcrypt")
    @Transactional
    void should_hash_password_with_bcrypt() {
        User user = createAndPersistUser("bcrypt@example.com");
        String token = UUID.randomUUID().toString();
        createAndPersistToken(token, user, Instant.now().plus(15, ChronoUnit.MINUTES));
        String newPassword = "MySecurePassword!123";
        passwordService.resetPassword(token, newPassword);
        User updatedUser = userRepository.find("email", "bcrypt@example.com").firstResult();
        assertThat(updatedUser.getPassword()).startsWith("$2a$");
        assertThat(BcryptUtil.matches(newPassword, updatedUser.getPassword())).isTrue();
    }
    private User createAndPersistUser(String email) {
        User user = User.builder()
                .email(email)
                .firstName("Test")
                .lastName("User")
                .password(BcryptUtil.bcryptHash("originalPassword"))
                .accountActivated(true)
                .roles(Set.of(testRole))
                .build();
        userRepository.persist(user);
        return user;
    }
    private void createAndPersistToken(String token, User user, Instant expiryDate) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .build();
        passwordResetTokenRepository.persist(resetToken);
    }
}
