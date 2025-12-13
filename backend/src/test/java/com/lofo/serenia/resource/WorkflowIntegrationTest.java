package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.conversation.MessageRole;
import com.lofo.serenia.domain.user.AccountActivationToken;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
import com.lofo.serenia.dto.out.ApiMessageResponse;
import com.lofo.serenia.repository.*;
import com.lofo.serenia.service.chat.ChatCompletionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Comprehensive workflow integration test covering:
 * 1. User registration with activation token
 * 2. Failed login before account activation
 * 3. Account activation
 * 4. Successful login after activation
 * 5. Retrieve user info via /me endpoint
 * 6. Send messages with mocked OpenAI responses
 * 7. Verify conversation persistence
 * 8. Message decryption via API
 * 9. Multi-user conversation isolation
 * 10. Access control verification (403 for unauthorized users)
 */
@QuarkusTest
@TestProfile(TestResourceProfile.class)
@DisplayName("Complete Workflow Integration Test")
class WorkflowIntegrationTest {

    private static final String USER1_EMAIL = "user1+workflow@example.com";
    private static final String USER1_PASSWORD = "WorkflowUser1!";
    private static final String USER1_FIRST_NAME = "Alice";
    private static final String USER1_LAST_NAME = "Dupont";

    private static final String USER2_EMAIL = "user2+workflow@example.com";
    private static final String USER2_PASSWORD = "WorkflowUser2!";
    private static final String USER2_FIRST_NAME = "Bob";
    private static final String USER2_LAST_NAME = "Martin";

    @InjectMock
    ChatCompletionService chatCompletionService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserTokenQuotaRepository userTokenQuotaRepository;

    @Inject
    UserTokenUsageRepository userTokenUsageRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    AccountActivationTokenRepository accountActivationTokenRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up database
        userTokenUsageRepository.deleteAll();
        userTokenQuotaRepository.deleteAll();
        conversationRepository.deleteAll();
        accountActivationTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Ensure role exists for new users
        Role userRole = Role.builder().name("USER").build();
        roleRepository.persist(userRole);
        roleRepository.flush();
    }

    @Test
    @DisplayName("validateWorkflow - Complete end-to-end workflow with multi-user isolation")
    void validateWorkflow() {
        // Mock OpenAI responses
        when(chatCompletionService.generateReply(any(), any()))
                .thenReturn("Bonjour ! Très heureux de discuter avec toi 🙂")
                .thenReturn("Bien sûr! Je serais ravi de t'aider. N'hésite pas à me poser tes questions...");

        // =============== PHASE 1: USER 1 REGISTRATION & ACTIVATION ===============

        // 1.1 Register User 1
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "firstName", USER1_FIRST_NAME,
                        "lastName", USER1_LAST_NAME,
                        "email", USER1_EMAIL,
                        "password", USER1_PASSWORD
                ))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .body()
                .as(ApiMessageResponse.class);

        // 1.2 Attempt login before activation → should fail with 401
        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(USER1_EMAIL, USER1_PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

        // 1.3 Extract activation token from database
        String activationToken = extractActivationTokenFromDatabase(USER1_EMAIL);
        assertThat(activationToken).isNotNull();

        // 1.4 Activate account
        given()
                .queryParam("token", activationToken)
                .when()
                .get("/api/auth/activate")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        // 1.5 Login after activation → should succeed
        var loginResponse = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(USER1_EMAIL, USER1_PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract();

        String user1Token = loginResponse.path("token");
        assertThat(user1Token).isNotNull();

        // 1.6 Verify user info via /me endpoint
        given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get("/api/auth/me")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("email", equalTo(USER1_EMAIL))
                .body("firstName", equalTo(USER1_FIRST_NAME))
                .body("lastName", equalTo(USER1_LAST_NAME));

        // =============== PHASE 2: USER 1 MESSAGING - FIRST MESSAGE ===============

        // 2.1 Send first message with User 1
        var firstMessageResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType(ContentType.JSON)
                .body(Map.of("content", "Bonjour"))
                .when()
                .post("/api/conversations/add-message")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract();

        String conversationId1 = firstMessageResponse.path("conversationId");
        String assistantMessage1 = firstMessageResponse.path("content");
        String role1 = firstMessageResponse.path("role");

        assertThat(conversationId1).isNotNull();
        assertThat(role1).isEqualTo(MessageRole.ASSISTANT.name());
        assertThat(assistantMessage1).contains("heureux");

        // =============== PHASE 3: USER 1 MESSAGING - SECOND MESSAGE ===============

        // 3.1 Send second message with User 1
        var secondMessageResponse = given()
                .header("Authorization", "Bearer " + user1Token)
                .contentType(ContentType.JSON)
                .body(Map.of("content", "Comment ça va?"))
                .when()
                .post("/api/conversations/add-message")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract();

        String conversationId2 = secondMessageResponse.path("conversationId");
        String assistantMessage2 = secondMessageResponse.path("content");

        assertThat(conversationId2).isEqualTo(conversationId1);
        assertThat(assistantMessage2).contains("ravi");

        // =============== PHASE 4: VERIFY CONVERSATION MESSAGES ===============

        // 4.1 Retrieve all messages for User 1's conversation
        List<Map<String, Object>> messages = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get("/api/conversations/" + conversationId1 + "/messages")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(new TypeRef<>() {});

        assertThat(messages).hasSize(4);

        // Verify message structure and decryption
        assertThat(messages.get(0).get("role")).isEqualTo(MessageRole.USER.name());
        assertThat(messages.get(0).get("content")).isEqualTo("Bonjour");

        assertThat(messages.get(1).get("role")).isEqualTo(MessageRole.ASSISTANT.name());
        assertThat(messages.get(1).get("content")).asString().contains("heureux");

        assertThat(messages.get(2).get("role")).isEqualTo(MessageRole.USER.name());
        assertThat(messages.get(2).get("content")).isEqualTo("Comment ça va?");

        assertThat(messages.get(3).get("role")).isEqualTo(MessageRole.ASSISTANT.name());
        assertThat(messages.get(3).get("content")).asString().contains("ravi");

        // =============== PHASE 5: USER 2 REGISTRATION & ACTIVATION ===============

        // 5.1 Register User 2
        var registerUser2Response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "firstName", USER2_FIRST_NAME,
                        "lastName", USER2_LAST_NAME,
                        "email", USER2_EMAIL,
                        "password", USER2_PASSWORD
                ))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract();

        // 5.2 Extract and activate User 2
        String activationToken2 = extractActivationTokenFromDatabase(USER2_EMAIL);

        given()
                .queryParam("token", activationToken2)
                .when()
                .get("/api/auth/activate")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        // 5.3 Login User 2
        var loginUser2Response = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(USER2_EMAIL, USER2_PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract();

        String user2Token = loginUser2Response.path("token");
        assertThat(user2Token).isNotNull();

        // =============== PHASE 6: USER 2 MESSAGING - CREATE NEW CONVERSATION ===============

        // 6.1 Send first message with User 2 (should create new conversation)
        var user2FirstMessageResponse = given()
                .header("Authorization", "Bearer " + user2Token)
                .contentType(ContentType.JSON)
                .body(Map.of("content", "Salut tout le monde"))
                .when()
                .post("/api/conversations/add-message")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract();

        String conversationId2User2 = user2FirstMessageResponse.path("conversationId");
        assertThat(conversationId2User2)
                .isNotNull()
                .isNotEqualTo(conversationId1);

        // =============== PHASE 7: ACCESS CONTROL - VERIFY 403 UNAUTHORIZED ===============

        // 7.1 User 2 tries to access User 1's conversation → should get 403
        given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get("/api/conversations/" + conversationId1 + "/messages")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

        // =============== PHASE 8: USER 2 RETRIEVES THEIR OWN MESSAGES ===============

        // 8.1 User 2 retrieves their conversation messages
        List<Map<String, Object>> user2Messages = given()
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get("/api/conversations/" + conversationId2User2 + "/messages")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(new TypeRef<>() {});

        assertThat(user2Messages).hasSize(2);
        assertThat(user2Messages.get(0).get("role")).isEqualTo(MessageRole.USER.name());
        assertThat(user2Messages.get(0).get("content")).isEqualTo("Salut tout le monde");
        assertThat(user2Messages.get(1).get("role")).isEqualTo(MessageRole.ASSISTANT.name());

        // =============== PHASE 9: USER 1 STILL HAS THEIR MESSAGES ===============

        // 9.1 Verify User 1 still has their 4 messages
        List<Map<String, Object>> user1FinalMessages = given()
                .header("Authorization", "Bearer " + user1Token)
                .when()
                .get("/api/conversations/" + conversationId1 + "/messages")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(new TypeRef<>() {});

        assertThat(user1FinalMessages).hasSize(4);
    }

    // =============== HELPER METHODS ===============

    private String extractActivationTokenFromDatabase(String email) {
        User user = userRepository.find("email", email).firstResultOptional().orElse(null);
        if (user == null) {
            return null;
        }
        return accountActivationTokenRepository.find("user.id", user.getId())
                .firstResultOptional()
                .map(AccountActivationToken::getToken)
                .orElse(null);
    }
}

