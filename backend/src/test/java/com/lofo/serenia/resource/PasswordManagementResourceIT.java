package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.ForgotPasswordRequest;
import com.lofo.serenia.rest.dto.in.ResetPasswordRequest;
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
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(TestResourceProfile.class)
class PasswordManagementResourceIT {

    @Inject
    UserRepository userRepository;

    private static final String FORGOT_PATH = "/api/password/forgot";
    private static final String RESET_PATH = "/api/password/reset";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String NEW_PASSWORD = "NewPassword456!";

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should_return_200_when_requesting_password_reset")
    void should_return_200_when_requesting_password_reset() {
        given()
                .contentType(ContentType.JSON)
                .body(new ForgotPasswordRequest(TEST_EMAIL))
                .when()
                .post(FORGOT_PATH)
                .then()
                .statusCode(200)
                .body("message", containsString("lien de réinitialisation"));
    }

    @Test
    @DisplayName("should_return_200_when_email_not_exists")
    void should_return_200_when_email_not_exists() {
        given()
                .contentType(ContentType.JSON)
                .body(new ForgotPasswordRequest("nonexistent@example.com"))
                .when()
                .post(FORGOT_PATH)
                .then()
                .statusCode(200)
                .body("message", containsString("lien de réinitialisation"));
    }

    @Test
    @DisplayName("should_return_400_when_invalid_payload")
    void should_return_400_when_invalid_payload() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"\"}")
                .when()
                .post(FORGOT_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_401_when_reset_password_with_invalid_token")
    void should_return_401_when_reset_password_with_invalid_token() {
        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest("invalid-token", NEW_PASSWORD))
                .when()
                .post(RESET_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_400_when_reset_password_with_empty_token")
    void should_return_400_when_reset_password_with_empty_token() {
        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest("", NEW_PASSWORD))
                .when()
                .post(RESET_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_reset_password_with_weak_password")
    void should_return_400_when_reset_password_with_weak_password() {
        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest("valid-token", "weak"))
                .when()
                .post(RESET_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_reset_password_with_empty_password")
    void should_return_400_when_reset_password_with_empty_password() {
        given()
                .contentType(ContentType.JSON)
                .body(new ResetPasswordRequest("valid-token", ""))
                .when()
                .post(RESET_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_reset_password_missing_required_fields")
    void should_return_400_when_reset_password_missing_required_fields() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"token\": \"some-token\"}")
                .when()
                .post(RESET_PATH)
                .then()
                .statusCode(400);
    }
}

