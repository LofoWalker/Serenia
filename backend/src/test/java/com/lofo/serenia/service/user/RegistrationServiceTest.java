package com.lofo.serenia.service.user;
import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.service.subscription.SubscriptionService;
import com.lofo.serenia.service.user.activation.AccountActivationService;
import com.lofo.serenia.service.user.registration.RegistrationService;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService Tests")
class RegistrationServiceTest {
    private static final String TEST_EMAIL = "newuser@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final String FRONTEND_URL = "http://localhost:4200";
    private static final long MAX_USERS = 100L;
    private static final String TEST_ACTIVATION_TOKEN = UUID.randomUUID().toString();
    @Mock
    private UserRepository userRepository;
    @Mock
    private SereniaConfig sereniaConfig;
    @Mock
    private AccountActivationService accountActivationService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private PanacheQuery<User> panacheQuery;
    private RegistrationService registrationService;
    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                sereniaConfig,
                accountActivationService,
                subscriptionService
        );
    }
    @Test
    @DisplayName("should_register_user_with_valid_data")
    void should_register_user_with_valid_data() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        verify(userRepository).persist(any(User.class));
        verify(accountActivationService).generateAndPersistActivationToken(any(User.class));
        verify(accountActivationService).sendActivationEmail(any(User.class), eq(FRONTEND_URL + "/activate?token=" + TEST_ACTIVATION_TOKEN));
    }
    @Test
    @DisplayName("should_throw_when_email_already_exists")
    void should_throw_when_email_already_exists() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        User existingUser = User.builder().email(TEST_EMAIL).build();
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.of(existingUser));
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(SereniaException.class)
                .hasMessageContaining("Email already exists");
        verify(userRepository, never()).persist(any(User.class));
    }
    @Test
    @DisplayName("should_throw_when_max_users_reached")
    void should_throw_when_max_users_reached() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(userRepository.count()).thenReturn(MAX_USERS);
        assertThatThrownBy(() -> registrationService.register(dto))
                .isInstanceOf(SereniaException.class)
                .hasMessageContaining("Registration closed");
        verify(userRepository, never()).persist(any(User.class));
    }
    @Test
    @DisplayName("should_hash_password_before_storing")
    void should_hash_password_before_storing() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).persist(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isNotEqualTo(TEST_PASSWORD);
        assertThat(savedUser.getPassword()).startsWith("$2");
    }
    @Test
    @DisplayName("should_set_account_as_not_activated")
    void should_set_account_as_not_activated() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).persist(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.isAccountActivated()).isFalse();
    }
    @Test
    @DisplayName("should_delegate_token_generation_to_activation_service")
    void should_delegate_token_generation_to_activation_service() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        verify(accountActivationService).generateAndPersistActivationToken(any(User.class));
    }
    @Test
    @DisplayName("should_delegate_email_sending_to_activation_service")
    void should_delegate_email_sending_to_activation_service() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        verify(accountActivationService).sendActivationEmail(any(User.class), anyString());
    }
    @Test
    @DisplayName("should_build_correct_activation_link")
    void should_build_correct_activation_link() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(accountActivationService).sendActivationEmail(any(User.class), linkCaptor.capture());
        String activationLink = linkCaptor.getValue();
        assertThat(activationLink).isEqualTo(FRONTEND_URL + "/activate?token=" + TEST_ACTIVATION_TOKEN);
    }
    @Test
    @DisplayName("should_create_user_with_correct_data")
    void should_create_user_with_correct_data() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO(TEST_LAST_NAME, TEST_FIRST_NAME, TEST_EMAIL, TEST_PASSWORD);
        when(sereniaConfig.maxUsers()).thenReturn(MAX_USERS);
        when(sereniaConfig.frontUrl()).thenReturn(FRONTEND_URL);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.find("email", TEST_EMAIL)).thenReturn(panacheQuery);
        when(panacheQuery.firstResultOptional()).thenReturn(Optional.empty());
        when(accountActivationService.generateAndPersistActivationToken(any(User.class))).thenReturn(TEST_ACTIVATION_TOKEN);
        registrationService.register(dto);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).persist(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(savedUser.getFirstName()).isEqualTo(TEST_FIRST_NAME);
        assertThat(savedUser.getLastName()).isEqualTo(TEST_LAST_NAME);
    }
}
