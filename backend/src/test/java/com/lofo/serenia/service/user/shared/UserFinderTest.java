package com.lofo.serenia.service.user.shared;

import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFinder Tests")
class UserFinderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PanacheQuery<User> query;

    private UserFinder userFinder;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TEST_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        userFinder = new UserFinder(userRepository);
    }

    @Nested
    @DisplayName("findByEmailOrThrow")
    class FindByEmailOrThrow {

        @Test
        @DisplayName("should return user when found")
        void should_return_user_when_found() {
            User user = createTestUser();
            when(userRepository.find("email", TEST_EMAIL)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(user));

            User result = userFinder.findByEmailOrThrow(TEST_EMAIL);

            assertThat(result).isEqualTo(user);
            assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("should throw not found when not exists")
        void should_throw_not_found_when_not_exists() {
            when(userRepository.find("email", TEST_EMAIL)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userFinder.findByEmailOrThrow(TEST_EMAIL))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("should return optional user when found")
        void should_return_optional_user() {
            User user = createTestUser();
            when(userRepository.find("email", TEST_EMAIL)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(user));

            Optional<User> result = userFinder.findByEmail(TEST_EMAIL);

            assertThat(result).isPresent().contains(user);
        }

        @Test
        @DisplayName("should return empty optional when not found")
        void should_return_empty_optional() {
            when(userRepository.find("email", TEST_EMAIL)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            Optional<User> result = userFinder.findByEmail(TEST_EMAIL);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("should return true when exists")
        void should_return_true_when_exists() {
            User user = createTestUser();
            when(userRepository.find("email", TEST_EMAIL)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(user));

            boolean result = userFinder.existsByEmail(TEST_EMAIL);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when not exists")
        void should_return_false_when_not_exists() {
            when(userRepository.find("email", TEST_EMAIL)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            boolean result = userFinder.existsByEmail(TEST_EMAIL);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdOrThrow")
    class FindByIdOrThrow {

        @Test
        @DisplayName("should return user by id")
        void should_return_user_by_id() {
            User user = createTestUser();
            when(userRepository.find("id", USER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(user));

            User result = userFinder.findByIdOrThrow(USER_ID);

            assertThat(result).isEqualTo(user);
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw when id not found")
        void should_throw_when_id_not_found() {
            when(userRepository.find("id", USER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userFinder.findByIdOrThrow(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    private User createTestUser() {
        return User.builder()
                .id(USER_ID)
                .email(TEST_EMAIL)
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .accountActivated(true)
                .build();
    }
}
