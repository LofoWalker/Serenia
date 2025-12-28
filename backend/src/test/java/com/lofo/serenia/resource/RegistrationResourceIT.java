package com.lofo.serenia.resource;
import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.user.BaseToken;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.BaseTokenRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.RegistrationRequestDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
@QuarkusTest
@TestProfile(TestResourceProfile.class)
class RegistrationResourceIT {
    @Inject
    UserRepository userRepository;
    @Inject
    BaseTokenRepository baseTokenRepository;
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String ACTIVATE_PATH = "/api/auth/activate";
    private static final String TEST_EMAIL = "newuser@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "Jane";
    private static final String TEST_LAST_NAME = "Smith";
    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        baseTokenRepository.deleteAll();
        userRepository.deleteAll();
    }
    // ============== REGISTER TESTS ==============
    @Test
    @DisplayName("should_register_user_and_return_201")
    void should_register_user_and_return_201() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegistrationRequestDTO(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD))
                .when()
                .post(REGISTER_PATH)
                .then()
                .statusCode(201)
                .body("message", containsString("Inscription"));
        User createdUser = userRepository.find("email", TEST_EMAIL).firstResult();
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.isAccountActivated()).isFalse();
    }
    @Test
    @DisplayName("should_create_and_persist_activation_token_on_registration")
    void should_create_and_persist_activation_token_on_registration() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegistrationRequestDTO(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD))
                .when()
                .post(REGISTER_PATH)
                .then()
                .statusCode(201);
        User createdUser = userRepository.find("email", TEST_EMAIL).firstResult();
        assertThat(createdUser).isNotNull();
        BaseToken activationToken = baseTokenRepository.find("user", createdUser).firstResult();
        assertThat(activationToken).isNotNull();
        assertThat(activationToken.getToken()).isNotNull();
        assertThat(activationToken.getToken()).isNotEmpty();
        assertThat(activationToken.getExpiryDate()).isAfter(Instant.now());
    }
    @Test
    @DisplayName("should_create_token_linked_to_correct_user_on_registration")
    void should_create_token_linked_to_correct_user_on_registration() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegistrationRequestDTO(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PASSWORD))
                .when()
                .post(REGISTER_PATH)
                .then()
                .statusCode(201);
        User createdUser = userRepository.find("email", TEST_EMAIL).firstResult();
        BaseToken activationToken = baseTokenRepository.find("user", createdUser).firstResult();
        assertThat(activationToken.getUser().getId()).isEqualTo(createdUser.getId());
        assertThat(activationToken.getUser().getEmail()).isEqualTo(TEST_EMAIL);
    }
    @Test
    @DisplayName("should_return_400_when_email_already_exists")
    void should_return_400_when_email_already_exists() {
        createExistingUser(TEST_EMAIL);
        given()
                .contentType(ContentType.JSON)
                .body(new RegistrationRequestDTO(TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME))
                .when()
                .post(REGISTER_PATH)
                .then()
                .statusCode(400);
    }
    @Test
    @DisplayName("should_return_400_when_invalid_email")
    void should_return_400_when_invalid_email() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegistrationRequestDTO("invalid-email", TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME))
                .when()
                .post(REGISTER_PATH)
                .then()
                .statusCode(400);
    }
    // ============== ACTIVATE TESTS ==============
    @Test
    @DisplayName("should_activate_account_with_valid_token")
    void should_activate_account_with_valid_token() {
        User user = createInactiveUser(TEST_EMAIL);
        String validToken = createValidActivationToken(user);
        given()
                .contentType(ContentType.JSON)
                .queryParam("token", validToken)
                .when()
                .get(ACTIVATE_PATH)
                .then()
                .statusCode(200)
                .body("message", containsString("Compte"));
        User activatedUser = userRepository.find("email", TEST_EMAIL).firstResult();
        assertThat(activatedUser.isAccountActivated()).isTrue();
    }
    @Test
    @DisplayName("should_return_400_when_token_is_null")
    void should_return_400_when_token_is_null() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(ACTIVATE_PATH)
                .then()
                .statusCode(400);
    }
    @Test
    @DisplayName("should_return_400_when_token_is_empty")
    void should_return_400_when_token_is_empty() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("token", "")
                .when()
                .get(ACTIVATE_PATH)
                .then()
                .statusCode(400);
    }
    @Test
    @DisplayName("should_return_401_when_token_is_invalid")
    void should_return_401_when_token_is_invalid() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("token", "invalid-token-that-does-not-exist")
                .when()
                .get(ACTIVATE_PATH)
                .then()
                .statusCode(401);
    }
    @Test
    @DisplayName("should_return_401_when_token_is_expired")
    void should_return_401_when_token_is_expired() {
        User user = createInactiveUser(TEST_EMAIL);
        String expiredToken = createExpiredActivationToken(user);
        given()
                .contentType(ContentType.JSON)
                .queryParam("token", expiredToken)
                .when()
                .get(ACTIVATE_PATH)
                .then()
                .statusCode(401);
        User stillInactiveUser = userRepository.find("email", TEST_EMAIL).firstResult();
        assertThat(stillInactiveUser.isAccountActivated()).isFalse();
    }
    // ============== HELPER METHODS ==============
    @Transactional
    void createExistingUser(String email) {
        User existingUser = User.builder()
                .email(email)
                .password("password")
                .firstName("Existing")
                .lastName("User")
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persistAndFlush(existingUser);
    }
    @Transactional
    User createInactiveUser(String email) {
        User user = User.builder()
                .email(email)
                .password(TEST_PASSWORD)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .accountActivated(false)
                .role(Role.USER)
                .build();
        userRepository.persistAndFlush(user);
        return user;
    }
    @Transactional
    String createValidActivationToken(User user) {
        String token = UUID.randomUUID().toString();
        BaseToken activationToken = BaseToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        baseTokenRepository.persistAndFlush(activationToken);
        return token;
    }
    @Transactional
    String createExpiredActivationToken(User user) {
        String token = UUID.randomUUID().toString();
        BaseToken activationToken = BaseToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        baseTokenRepository.persistAndFlush(activationToken);
        return token;
    }
}
