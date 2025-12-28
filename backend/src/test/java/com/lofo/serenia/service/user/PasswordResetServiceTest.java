package com.lofo.serenia.service.user;
import com.lofo.serenia.exception.exceptions.InvalidTokenException;
import com.lofo.serenia.persistence.entity.user.BaseToken;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.BaseTokenRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.service.mail.provider.EmailTemplateProvider;
import com.lofo.serenia.service.mail.MailSender;
import com.lofo.serenia.service.user.password.PasswordResetService;
import com.lofo.serenia.service.user.shared.UserFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Tests")
class PasswordResetServiceTest {
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String FRONTEND_URL = "http://localhost:4200";
    private static final String NEW_PASSWORD = "NewSecurePassword123!";
    @Mock
    private BaseTokenRepository baseTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailTemplateProvider emailTemplateProvider;
    @Mock
    private MailSender mailSender;
    @Mock
    private UserFinder userFinder;
    private PasswordResetService passwordResetService;
    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                baseTokenRepository,
                userRepository,
                emailTemplateProvider,
                mailSender,
                userFinder,
                FRONTEND_URL
        );
    }
    @Test
    @DisplayName("should_generate_reset_token_for_existing_user")
    void should_generate_reset_token_for_existing_user() {
        User user = createTestUser();
        when(userFinder.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(emailTemplateProvider.getPasswordResetEmailSubject()).thenReturn("Reset Password");
        when(emailTemplateProvider.getPasswordResetEmailBody(any(), any())).thenReturn("<html>Reset</html>");
        passwordResetService.requestPasswordReset(TEST_EMAIL);
        ArgumentCaptor<BaseToken> tokenCaptor = ArgumentCaptor.forClass(BaseToken.class);
        verify(baseTokenRepository).persist(tokenCaptor.capture());
        BaseToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiryDate()).isAfter(Instant.now());
    }
    @Test
    @DisplayName("should_send_email_when_user_exists")
    void should_send_email_when_user_exists() {
        User user = createTestUser();
        when(userFinder.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(emailTemplateProvider.getPasswordResetEmailSubject()).thenReturn("Reset Password");
        when(emailTemplateProvider.getPasswordResetEmailBody(any(), any())).thenReturn("<html>Reset</html>");
        passwordResetService.requestPasswordReset(TEST_EMAIL);
        verify(mailSender).sendHtml(eq(TEST_EMAIL), eq("Reset Password"), any());
    }
    @Test
    @DisplayName("should_not_send_email_when_user_not_exists")
    void should_not_send_email_when_user_not_exists() {
        when(userFinder.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());
        passwordResetService.requestPasswordReset(TEST_EMAIL);
        verify(mailSender, never()).sendHtml(any(), any(), any());
        verify(baseTokenRepository, never()).persist(any(BaseToken.class));
    }
    @Test
    @DisplayName("should_delete_previous_tokens_before_creating_new")
    void should_delete_previous_tokens_before_creating_new() {
        User user = createTestUser();
        when(userFinder.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(emailTemplateProvider.getPasswordResetEmailSubject()).thenReturn("Reset Password");
        when(emailTemplateProvider.getPasswordResetEmailBody(any(), any())).thenReturn("<html>Reset</html>");
        passwordResetService.requestPasswordReset(TEST_EMAIL);
        verify(baseTokenRepository).deleteByUserId(user.getId());
    }
    @Test
    @DisplayName("should_reset_password_with_valid_token")
    void should_reset_password_with_valid_token() {
        User user = createTestUser();
        String oldPassword = user.getPassword();
        BaseToken validToken = createValidToken(user);
        when(baseTokenRepository.findByToken(validToken.getToken())).thenReturn(Optional.of(validToken));
        passwordResetService.resetPassword(validToken.getToken(), NEW_PASSWORD);
        assertThat(user.getPassword()).isNotEqualTo(oldPassword);
        verify(userRepository).persist(user);
        verify(baseTokenRepository).delete(validToken);
    }
    @Test
    @DisplayName("should_throw_when_token_not_found")
    void should_throw_when_token_not_found() {
        when(baseTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", NEW_PASSWORD))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
    }
    @Test
    @DisplayName("should_throw_when_token_expired")
    void should_throw_when_token_expired() {
        User user = createTestUser();
        BaseToken expiredToken = createExpiredToken(user);
        when(baseTokenRepository.findByToken(expiredToken.getToken())).thenReturn(Optional.of(expiredToken));
        assertThatThrownBy(() -> passwordResetService.resetPassword(expiredToken.getToken(), NEW_PASSWORD))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired token");
        verify(baseTokenRepository).delete(expiredToken);
    }
    @Test
    @DisplayName("should_hash_new_password")
    void should_hash_new_password() {
        User user = createTestUser();
        BaseToken validToken = createValidToken(user);
        when(baseTokenRepository.findByToken(validToken.getToken())).thenReturn(Optional.of(validToken));
        passwordResetService.resetPassword(validToken.getToken(), NEW_PASSWORD);
        assertThat(user.getPassword()).isNotEqualTo(NEW_PASSWORD);
        assertThat(user.getPassword()).startsWith("$2");
    }
    @Test
    @DisplayName("should_build_correct_reset_link")
    void should_build_correct_reset_link() {
        User user = createTestUser();
        when(userFinder.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
        when(emailTemplateProvider.getPasswordResetEmailSubject()).thenReturn("Reset Password");
        when(emailTemplateProvider.getPasswordResetEmailBody(any(), any())).thenReturn("<html>Reset</html>");
        passwordResetService.requestPasswordReset(TEST_EMAIL);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailTemplateProvider).getPasswordResetEmailBody(eq(TEST_FIRST_NAME), linkCaptor.capture());
        String resetLink = linkCaptor.getValue();
        assertThat(resetLink).startsWith(FRONTEND_URL + "/reset-password?token=");
    }
    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .password("hashedPassword")
                .firstName(TEST_FIRST_NAME)
                .lastName("Doe")
                .accountActivated(true)
                .role(Role.USER)
                .build();
    }
    private BaseToken createValidToken(User user) {
        return BaseToken.builder()
                .id(UUID.randomUUID())
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();
    }
    private BaseToken createExpiredToken(User user) {
        return BaseToken.builder()
                .id(UUID.randomUUID())
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }
}
