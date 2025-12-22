package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.util.JwtTestTokenGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(TestResourceProfile.class)
class ProfileResourceIT {

    @Inject
    UserRepository userRepository;

    private static final String PROFILE_PATH = "/api/profile";
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
    @DisplayName("should_return_401_when_getting_profile_without_auth")
    void should_return_401_when_getting_profile_without_auth() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(PROFILE_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_deleting_profile_without_auth")
    void should_return_401_when_deleting_profile_without_auth() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete(PROFILE_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_with_invalid_jwt_token")
    void should_return_401_with_invalid_jwt_token() {
        createUser(TEST_EMAIL);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2("invalid-token")
                .when()
                .get(PROFILE_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_deleting_with_invalid_jwt_token")
    void should_return_401_when_deleting_with_invalid_jwt_token() {
        createUser(TEST_EMAIL);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2("invalid-token")
                .when()
                .delete(PROFILE_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_200_and_user_profile_when_authenticated")
    void should_return_200_and_user_profile_when_authenticated() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(PROFILE_PATH)
                .then()
                .statusCode(200)
                .body("email", equalTo(TEST_EMAIL))
                .body("firstName", equalTo(TEST_FIRST_NAME))
                .body("lastName", equalTo(TEST_LAST_NAME))
                .body("role", equalTo("USER"));
    }

    @Test
    @DisplayName("should_return_204_when_deleting_authenticated_user_account")
    void should_return_204_when_deleting_authenticated_user_account() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(PROFILE_PATH)
                .then()
                .statusCode(204);

        // Verify the user has been deleted
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(PROFILE_PATH)
                .then()
                .statusCode(401);
    }

    @Transactional
    User createAndPersistUser(String email) {
        User user = User.builder()
                .email(email)
                .password(TEST_PASSWORD)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persistAndFlush(user);
        return user;
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

