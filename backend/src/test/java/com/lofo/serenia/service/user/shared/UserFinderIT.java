package com.lofo.serenia.service.user.shared;
import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@QuarkusTest
@TestProfile(TestResourceProfile.class)
@DisplayName("UserFinder Integration Tests")
class UserFinderIT {
    @Inject
    UserFinder userFinder;
    @Inject
    UserRepository userRepository;
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    @BeforeEach
    @Transactional
    void setup() {
        userRepository.deleteAll();
    }
    @Test
    @DisplayName("should_find_user_by_email_when_exists")
    void should_find_user_by_email_when_exists() {
        createUser(TEST_EMAIL);
        User user = userFinder.findByEmailOrThrow(TEST_EMAIL);
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(user.getFirstName()).isEqualTo(TEST_FIRST_NAME);
        assertThat(user.getLastName()).isEqualTo(TEST_LAST_NAME);
    }
    @Test
    @DisplayName("should_throw_not_found_when_finding_nonexistent_user")
    void should_throw_not_found_when_finding_nonexistent_user() {
        assertThatThrownBy(() -> userFinder.findByEmailOrThrow("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }
    @Test
    @DisplayName("should_return_optional_user_when_exists")
    void should_return_optional_user_when_exists() {
        createUser(TEST_EMAIL);
        Optional<User> userOpt = userFinder.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        assertThat(userOpt.get().getEmail()).isEqualTo(TEST_EMAIL);
    }
    @Test
    @DisplayName("should_return_empty_optional_when_user_not_exists")
    void should_return_empty_optional_when_user_not_exists() {
        Optional<User> userOpt = userFinder.findByEmail("nonexistent@example.com");
        assertThat(userOpt).isEmpty();
    }
    @Test
    @DisplayName("should_return_true_when_user_exists_by_email")
    void should_return_true_when_user_exists_by_email() {
        createUser(TEST_EMAIL);
        boolean exists = userFinder.existsByEmail(TEST_EMAIL);
        assertThat(exists).isTrue();
    }
    @Test
    @DisplayName("should_return_false_when_user_not_exists_by_email")
    void should_return_false_when_user_not_exists_by_email() {
        boolean exists = userFinder.existsByEmail("nonexistent@example.com");
        assertThat(exists).isFalse();
    }
    @Transactional
    void createUser(String email) {
        User user = User.builder()
                .email(email)
                .password(TEST_PASSWORD)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persistAndFlush(user);
    }
}
