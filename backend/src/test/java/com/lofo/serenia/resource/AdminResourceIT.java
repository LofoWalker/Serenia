package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.util.JwtTestTokenGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestProfile(TestResourceProfile.class)
@DisplayName("AdminResource Integration Tests")
class AdminResourceIT {

    @Inject
    UserRepository userRepository;

    private UUID adminUserId;
    private UUID regularUserId;
    private static final String ADMIN_EMAIL = "admin@serenia.com";
    private static final String USER_EMAIL = "user@serenia.com";

    @BeforeEach
    @Transactional
    void setup() {
        userRepository.deleteAll();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        User admin = User.builder()
                .email(ADMIN_EMAIL)
                .password("hashedpassword")
                .firstName("Admin")
                .lastName("User")
                .accountActivated(true)
                .role(Role.ADMIN)
                .build();
        userRepository.persist(admin);
        adminUserId = admin.getId();

        User regularUser = User.builder()
                .email(USER_EMAIL)
                .password("hashedpassword")
                .firstName("Regular")
                .lastName("User")
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persist(regularUser);
        regularUserId = regularUser.getId();
    }

    @Test
    @DisplayName("should return dashboard for admin user")
    void should_return_dashboard_for_admin_user() {
        String token = JwtTestTokenGenerator.generateToken(ADMIN_EMAIL, adminUserId, "ADMIN");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/admin/dashboard")
                .then()
                .statusCode(200)
                .body("users.totalUsers", greaterThanOrEqualTo(0))
                .body("messages.totalUserMessages", greaterThanOrEqualTo(0))
                .body("engagement.activeUsers", greaterThanOrEqualTo(0))
                .body("subscriptions.totalTokensConsumed", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("should deny access for regular user")
    void should_deny_access_for_regular_user() {
        String token = JwtTestTokenGenerator.generateToken(USER_EMAIL, regularUserId, "USER");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/admin/dashboard")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("should deny access without authentication")
    void should_deny_access_without_authentication() {
        given()
                .when()
                .get("/api/admin/dashboard")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should return timeline for admin")
    void should_return_timeline_for_admin() {
        String token = JwtTestTokenGenerator.generateToken(ADMIN_EMAIL, adminUserId, "ADMIN");

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("metric", "messages")
                .queryParam("days", 7)
                .when()
                .get("/api/admin/timeline")
                .then()
                .statusCode(200)
                .body("metric", equalTo("messages"))
                .body("data", hasSize(7));
    }

    @Test
    @DisplayName("should return 30 days timeline when requested")
    void should_return_30_days_timeline_when_requested() {
        String token = JwtTestTokenGenerator.generateToken(ADMIN_EMAIL, adminUserId, "ADMIN");

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("metric", "users")
                .queryParam("days", 30)
                .when()
                .get("/api/admin/timeline")
                .then()
                .statusCode(200)
                .body("metric", equalTo("users"))
                .body("data", hasSize(30));
    }

    @Test
    @DisplayName("should return user list for admin")
    void should_return_user_list_for_admin() {
        String token = JwtTestTokenGenerator.generateToken(ADMIN_EMAIL, adminUserId, "ADMIN");

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/admin/users")
                .then()
                .statusCode(200)
                .body("users", hasSize(greaterThan(0)))
                .body("totalCount", greaterThan(0))
                .body("page", equalTo(0))
                .body("size", equalTo(10));
    }

    @Test
    @DisplayName("should return user by email for admin")
    void should_return_user_by_email_for_admin() {
        String token = JwtTestTokenGenerator.generateToken(ADMIN_EMAIL, adminUserId, "ADMIN");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/admin/users/" + USER_EMAIL)
                .then()
                .statusCode(200)
                .body("email", equalTo(USER_EMAIL))
                .body("firstName", equalTo("Regular"))
                .body("lastName", equalTo("User"))
                .body("role", equalTo("USER"));
    }

    @Test
    @DisplayName("should return 404 for non-existent user")
    void should_return_404_for_non_existent_user() {
        String token = JwtTestTokenGenerator.generateToken(ADMIN_EMAIL, adminUserId, "ADMIN");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/admin/users/nonexistent@example.com")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("should deny user list access for regular user")
    void should_deny_user_list_access_for_regular_user() {
        String token = JwtTestTokenGenerator.generateToken(USER_EMAIL, regularUserId, "USER");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/admin/users")
                .then()
                .statusCode(403);
    }
}

