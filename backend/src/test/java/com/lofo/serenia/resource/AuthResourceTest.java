package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.in.RegistrationRequestDTO;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.service.auth.impl.EmailVerificationServiceImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("AuthResource integration tests")
@Tag("integration")
@TestProfile(TestResourceProfile.class)
class AuthResourceTest {

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    EntityManager em;

    private Role testRole;

    @BeforeEach
    @Transactional
    void setup() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        testRole = Role.builder().name("USER").build();
        roleRepository.persist(testRole);
    }

    @Transactional
    protected void persistUser(User user) {
        userRepository.persist(user);
        em.flush();
    }

    @Transactional
    protected void persistRole(Role role) {
        roleRepository.persist(role);
        em.flush();
    }

    @Test
    @DisplayName("should_return_201_created_on_successful_registration")
    void should_return_201_created_on_successful_registration() {
        RegistrationRequestDTO request = new RegistrationRequestDTO(
                "Dupont", "Jean", "jean@example.com", "Password123!"
        );

        given()
                .contentType("application/json")
                .body(request)
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("message", containsString("Inscription réussie"));
    }

    @Test
    @DisplayName("should_return_409_when_email_already_registered")
    void should_return_409_when_email_already_registered() {
        String email = "duplicate@example.com";
        User existingUser = User.builder()
                .email(email)
                .firstName("Existing")
                .lastName("User")
                .password(BCrypt.hashpw("Password123!", BCrypt.gensalt()))
                .accountActivated(true)
                .roles(Set.of(testRole))
                .build();
        persistUser(existingUser);

        RegistrationRequestDTO request = new RegistrationRequestDTO(
                "New", "User", email, "Password456!"
        );

        given()
                .contentType("application/json")
                .body(request)
                .post("/api/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("should_return_200_when_token_is_valid_and_not_expired")
    void should_return_200_when_token_is_valid_and_not_expired() {
        String token = EmailVerificationServiceImpl.generateActivationToken();
        User user = User.builder()
                .email("activate@example.com")
                .firstName("Activate")
                .lastName("Test")
                .password(BCrypt.hashpw("Password123!", BCrypt.gensalt()))
                .accountActivated(false)
                .activationToken(token)
                .tokenExpirationDate(EmailVerificationServiceImpl.calculateExpirationDate(1440))
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        given()
                .get("/api/auth/activate?token=" + token)
                .then()
                .statusCode(200)
                .body("message", containsString("Compte activé avec succès"));
    }

    @Test
    @DisplayName("should_return_400_when_token_is_invalid")
    void should_return_400_when_token_is_invalid() {
        given()
                .get("/api/auth/activate?token=invalid-token-123")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_token_is_expired")
    void should_return_400_when_token_is_expired() {
        String token = EmailVerificationServiceImpl.generateActivationToken();
        User user = User.builder()
                .email("expired@example.com")
                .firstName("Expired")
                .lastName("Test")
                .password(BCrypt.hashpw("Password123!", BCrypt.gensalt()))
                .accountActivated(false)
                .activationToken(token)
                .tokenExpirationDate(EmailVerificationServiceImpl.calculateExpirationDate(-60))
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        given()
                .get("/api/auth/activate?token=" + token)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_token_is_missing")
    void should_return_400_when_token_is_missing() {
        given()
                .get("/api/auth/activate?token=")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_200_with_jwt_when_credentials_valid_and_account_activated")
    void should_return_200_with_jwt_when_credentials_valid_and_account_activated() {
        String email = "login@example.com";
        String password = "Password123!";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = User.builder()
                .email(email)
                .firstName("Login")
                .lastName("Test")
                .password(hashedPassword)
                .accountActivated(true)
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        LoginRequestDTO request = new LoginRequestDTO(email, password);

        given()
                .contentType("application/json")
                .body(request)
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.firstName", equalTo("Login"));
    }

    @Test
    @DisplayName("should_return_401_when_account_not_activated")
    void should_return_401_when_account_not_activated() {
        String email = "notactivated@example.com";
        String password = "Password123!";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String token = EmailVerificationServiceImpl.generateActivationToken();

        User user = User.builder()
                .email(email)
                .firstName("NotActivated")
                .lastName("Test")
                .password(hashedPassword)
                .accountActivated(false)
                .activationToken(token)
                .tokenExpirationDate(EmailVerificationServiceImpl.calculateExpirationDate(1440))
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        LoginRequestDTO request = new LoginRequestDTO(email, password);

        given()
                .contentType("application/json")
                .body(request)
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_password_incorrect")
    void should_return_401_when_password_incorrect() {
        String email = "password@example.com";
        String correctPassword = "CorrectPassword123!";
        String wrongPassword = "WrongPassword456!";

        User user = User.builder()
                .email(email)
                .firstName("Password")
                .lastName("Test")
                .password(BCrypt.hashpw(correctPassword, BCrypt.gensalt()))
                .accountActivated(true)
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        LoginRequestDTO request = new LoginRequestDTO(email, wrongPassword);

        given()
                .contentType("application/json")
                .body(request)
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_email_not_found")
    void should_return_401_when_email_not_found() {
        LoginRequestDTO request = new LoginRequestDTO("unknown@example.com", "Password123!");

        given()
                .contentType("application/json")
                .body(request)
                .post("/api/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_200_with_user_info_when_authenticated")
    void should_return_200_with_user_info_when_authenticated() {
        String email = "me@example.com";
        String password = "Password123!";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = User.builder()
                .email(email)
                .firstName("Me")
                .lastName("User")
                .password(hashedPassword)
                .accountActivated(true)
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        String token = given()
                .contentType("application/json")
                .body(new LoginRequestDTO(email, password))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .get("/api/auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("firstName", equalTo("Me"))
                .body("lastName", equalTo("User"));
    }

    @Test
    @DisplayName("should_return_401_when_not_authenticated_on_me")
    void should_return_401_when_not_authenticated_on_me() {
        given()
                .get("/api/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_token_invalid_on_me")
    void should_return_401_when_token_invalid_on_me() {
        given()
                .header("Authorization", "Bearer invalid.token.here")
                .get("/api/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_204_when_account_deleted_successfully")
    void should_return_204_when_account_deleted_successfully() {
        String email = "delete@example.com";
        String password = "Password123!";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = User.builder()
                .email(email)
                .firstName("Delete")
                .lastName("User")
                .password(hashedPassword)
                .accountActivated(true)
                .roles(Set.of(testRole))
                .build();
        persistUser(user);

        String token = given()
                .contentType("application/json")
                .body(new LoginRequestDTO(email, password))
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .delete("/api/auth/me")
                .then()
                .statusCode(204);

        User deletedUser = userRepository.find("email", email).firstResultOptional().orElse(null);
        assert deletedUser == null : "User should be deleted from database";
    }

    @Test
    @DisplayName("should_return_401_when_not_authenticated_on_deleteMe")
    void should_return_401_when_not_authenticated_on_deleteMe() {
        given()
                .delete("/api/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_token_invalid_on_deleteMe")
    void should_return_401_when_token_invalid_on_deleteMe() {
        given()
                .header("Authorization", "Bearer invalid.token.here")
                .delete("/api/auth/me")
                .then()
                .statusCode(401);
    }
}

