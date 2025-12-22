package com.lofo.serenia.service.user;
import com.lofo.serenia.exception.exceptions.AuthenticationFailedException;
import com.lofo.serenia.exception.exceptions.UnactivatedAccountException;
import com.lofo.serenia.mapper.UserMapper;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.rest.dto.in.LoginRequestDTO;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    @Mock
    private UserFinder userFinder;
    @Mock
    private UserMapper userMapper;
    private AuthenticationService authenticationService;
    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userFinder, userMapper);
    }
    @Test
    @DisplayName("should_return_user_dto_when_credentials_valid")
    void should_return_user_dto_when_credentials_valid() {
        User user = createActivatedUser();
        UserResponseDTO expectedDto = createUserResponseDTO(user);
        LoginRequestDTO loginDto = new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD);
        when(userFinder.findByEmailOrThrow(TEST_EMAIL)).thenReturn(user);
        when(userMapper.toView(user)).thenReturn(expectedDto);
        UserResponseDTO result = authenticationService.login(loginDto);
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo(TEST_EMAIL);
    }
    @Test
    @DisplayName("should_throw_not_found_when_user_does_not_exist")
    void should_throw_not_found_when_user_does_not_exist() {
        LoginRequestDTO loginDto = new LoginRequestDTO("nonexistent@example.com", TEST_PASSWORD);
        when(userFinder.findByEmailOrThrow("nonexistent@example.com"))
                .thenThrow(new NotFoundException("User not found"));
        assertThatThrownBy(() -> authenticationService.login(loginDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }
    @Test
    @DisplayName("should_throw_authentication_failed_when_password_incorrect")
    void should_throw_authentication_failed_when_password_incorrect() {
        User user = createActivatedUser();
        LoginRequestDTO loginDto = new LoginRequestDTO(TEST_EMAIL, "WrongPassword!");
        when(userFinder.findByEmailOrThrow(TEST_EMAIL)).thenReturn(user);
        assertThatThrownBy(() -> authenticationService.login(loginDto))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid credentials");
    }
    @Test
    @DisplayName("should_throw_unactivated_account_when_not_activated")
    void should_throw_unactivated_account_when_not_activated() {
        User user = createInactiveUser();
        LoginRequestDTO loginDto = new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD);
        when(userFinder.findByEmailOrThrow(TEST_EMAIL)).thenReturn(user);
        assertThatThrownBy(() -> authenticationService.login(loginDto))
                .isInstanceOf(UnactivatedAccountException.class);
    }
    private User createActivatedUser() {
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt());
        return User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .password(hashedPassword)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .accountActivated(true)
                .role(Role.USER)
                .build();
    }
    private User createInactiveUser() {
        String hashedPassword = BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt());
        return User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .password(hashedPassword)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .accountActivated(false)
                .role(Role.USER)
                .build();
    }
    private UserResponseDTO createUserResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getLastName(), user.getFirstName(), user.getEmail(), "USER");
    }
}
