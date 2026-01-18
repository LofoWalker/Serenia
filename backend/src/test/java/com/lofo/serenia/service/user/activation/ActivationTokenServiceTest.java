package com.lofo.serenia.service.user.activation;

import com.lofo.serenia.exception.exceptions.InvalidTokenException;
import com.lofo.serenia.persistence.entity.user.BaseToken;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.BaseTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivationTokenService Unit Tests")
class ActivationTokenServiceTest {

    @Mock
    private BaseTokenRepository baseTokenRepository;

    private ActivationTokenService activationTokenService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        activationTokenService = new ActivationTokenService(baseTokenRepository);
    }

    @Nested
    @DisplayName("generateAndPersistActivationToken")
    class GenerateAndPersistActivationToken {

        @Test
        @DisplayName("should generate unique token")
        void should_generate_unique_token() {
            User user = createUser();

            String token = activationTokenService.generateAndPersistActivationToken(user);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(UUID.fromString(token)).isNotNull();
        }

        @Test
        @DisplayName("should persist token with correct expiry")
        void should_persist_token_with_correct_expiry() {
            User user = createUser();
            ArgumentCaptor<BaseToken> tokenCaptor = ArgumentCaptor.forClass(BaseToken.class);

            activationTokenService.generateAndPersistActivationToken(user);

            verify(baseTokenRepository).persist(tokenCaptor.capture());
            BaseToken capturedToken = tokenCaptor.getValue();

            assertThat(capturedToken.getUser()).isEqualTo(user);
            assertThat(capturedToken.getExpiryDate()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));
            assertThat(capturedToken.getExpiryDate()).isBefore(Instant.now().plus(25, ChronoUnit.HOURS));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("should return user for valid token")
        void should_return_user_for_valid_token() {
            User user = createUser();
            String tokenStr = UUID.randomUUID().toString();
            BaseToken token = createValidToken(user, tokenStr);

            when(baseTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

            User result = activationTokenService.validateToken(tokenStr);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("should throw when token not found")
        void should_throw_when_token_not_found() {
            String tokenStr = UUID.randomUUID().toString();
            when(baseTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activationTokenService.validateToken(tokenStr))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired token");
        }

        @Test
        @DisplayName("should throw when token expired")
        void should_throw_when_token_expired() {
            User user = createUser();
            String tokenStr = UUID.randomUUID().toString();
            BaseToken expiredToken = createExpiredToken(user, tokenStr);

            when(baseTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> activationTokenService.validateToken(tokenStr))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired token");
        }
    }

    @Nested
    @DisplayName("consumeToken")
    class ConsumeToken {

        @Test
        @DisplayName("should delete token after consumption")
        void should_delete_token_after_consumption() {
            String tokenStr = UUID.randomUUID().toString();

            activationTokenService.consumeToken(tokenStr);

            verify(baseTokenRepository).deleteByToken(tokenStr);
        }
    }

    private User createUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("hashedPassword");
        user.setRole(Role.USER);
        user.setAccountActivated(false);
        return user;
    }

    private BaseToken createValidToken(User user, String tokenStr) {
        return BaseToken.builder()
                .id(UUID.randomUUID())
                .token(tokenStr)
                .user(user)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
    }

    private BaseToken createExpiredToken(User user, String tokenStr) {
        return BaseToken.builder()
                .id(UUID.randomUUID())
                .token(tokenStr)
                .user(user)
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }
}
