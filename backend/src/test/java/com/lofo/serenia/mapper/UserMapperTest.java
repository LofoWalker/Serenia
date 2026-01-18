package com.lofo.serenia.mapper;

import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper Unit Tests")
class UserMapperTest {

    private UserMapper userMapper;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper() {};
    }

    @Nested
    @DisplayName("toView")
    class ToView {

        @Test
        @DisplayName("should map user to dto")
        void should_map_user_to_dto() {
            User user = createUser(Role.USER);

            UserResponseDTO result = userMapper.toView(user);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo(EMAIL);
            assertThat(result.firstName()).isEqualTo(FIRST_NAME);
            assertThat(result.lastName()).isEqualTo(LAST_NAME);
        }

        @Test
        @DisplayName("should return null when user is null")
        void should_return_null_when_user_is_null() {
            UserResponseDTO result = userMapper.toView(null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should map role correctly")
        void should_map_role_correctly() {
            User userWithUserRole = createUser(Role.USER);
            User userWithAdminRole = createUser(Role.ADMIN);

            UserResponseDTO userResult = userMapper.toView(userWithUserRole);
            UserResponseDTO adminResult = userMapper.toView(userWithAdminRole);

            assertThat(userResult.role()).isEqualTo("USER");
            assertThat(adminResult.role()).isEqualTo("ADMIN");
        }
    }

    private User createUser(Role role) {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setPassword("hashedPassword");
        user.setRole(role);
        user.setAccountActivated(true);
        return user;
    }
}
