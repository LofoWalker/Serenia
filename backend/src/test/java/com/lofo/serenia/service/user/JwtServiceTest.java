package com.lofo.serenia.service.user;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.service.user.jwt.JwtService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
@QuarkusTest
@TestProfile(TestResourceProfile.class)
@DisplayName("TokenService Tests")
class JwtServiceTest {
    @Inject
    JwtService jwtService;
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final String TEST_ROLE = "USER";
    @Test
    @DisplayName("should_generate_valid_jwt_token")
    void should_generate_valid_jwt_token() {
        UserResponseDTO user = createTestUser();
        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }
    @Test
    @DisplayName("should_generate_token_with_three_parts")
    void should_generate_token_with_three_parts() {
        UserResponseDTO user = createTestUser();
        String token = jwtService.generateToken(user);
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
    }
    @Test
    @DisplayName("should_generate_different_tokens_for_different_users")
    void should_generate_different_tokens_for_different_users() {
        UserResponseDTO user1 = createTestUser();
        UserResponseDTO user2 = new UserResponseDTO(UUID.randomUUID(), "other@example.com", "Jane", "Smith", "USER");
        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);
        assertThat(token1).isNotEqualTo(token2);
    }
    @Test
    @DisplayName("should_generate_token_for_admin_role")
    void should_generate_token_for_admin_role() {
        UserResponseDTO adminUser = new UserResponseDTO(TEST_USER_ID, TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, "ADMIN");
        String token = jwtService.generateToken(adminUser);
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }
    private UserResponseDTO createTestUser() {
        return new UserResponseDTO(TEST_USER_ID, TEST_EMAIL, TEST_FIRST_NAME, TEST_LAST_NAME, TEST_ROLE);
    }
}
