package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.LoginRequestDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(TestResourceProfile.class)
class AuthenticationResourceIT {

    @Inject
    UserRepository userRepository;

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should_return_token_when_credentials_valid")
    void should_return_token_when_credentials_valid() {
        persistUser(TEST_EMAIL, TEST_PASSWORD, true);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD))
                .when()
                .post(LOGIN_PATH)
                .then()
                .statusCode(200)
                .body("user.email", equalTo(TEST_EMAIL))
                .body("user.firstName", equalTo(TEST_FIRST_NAME))
                .body("user.lastName", equalTo(TEST_LAST_NAME))
                .body("token", notNullValue())
                .body("token", not(emptyString()));
    }

    @Test
    @DisplayName("should_return_401_when_password_invalid")
    void should_return_401_when_password_invalid() {
        persistUser(TEST_EMAIL, TEST_PASSWORD, true);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(TEST_EMAIL, "WrongPassword123!"))
                .when()
                .post(LOGIN_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_user_not_found")
    void should_return_401_when_user_not_found() {
        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO("nonexistent@example.com", TEST_PASSWORD))
                .when()
                .post(LOGIN_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_account_not_activated")
    void should_return_401_when_account_not_activated() {
        persistUser(TEST_EMAIL, TEST_PASSWORD, false);

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD))
                .when()
                .post(LOGIN_PATH)
                .then()
                .statusCode(401);
    }

    @Transactional
    void persistUser(String email, String password, boolean activated) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = User.builder()
                .email(email)
                .password(hashedPassword)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .accountActivated(activated)
                .role(Role.USER)
                .build();
        userRepository.persistAndFlush(user);
    }

    @Test
    @DisplayName("should_return_400_when_invalid_payload")
    void should_return_400_when_invalid_payload() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"\"}")
                .when()
                .post(LOGIN_PATH)
                .then()
                .statusCode(400);
    }
}

