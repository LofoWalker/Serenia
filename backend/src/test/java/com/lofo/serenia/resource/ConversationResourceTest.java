package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.domain.conversation.MessageRole;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.in.LoginRequestDTO;
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
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests the REST contract exposed by {@link ConversationResource}.
 */
@QuarkusTest
@TestProfile(TestResourceProfile.class)
class ConversationResourceTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "Password123!";

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
    ConversationTestHelper conversationTestHelper;

    private String bearerToken;
    private User currentUser;

    @BeforeEach
    void setUpIdentity() {
        setupDatabase();
        obtainToken();
    }

    @Transactional
    void setupDatabase() {
        userTokenUsageRepository.deleteAll();
        userTokenQuotaRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.deleteAll();
        Role testRole = Role.builder().name("USER").build();
        roleRepository.persist(testRole);

        String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
        User user = new User();
        user.setEmail(EMAIL);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword(hashedPassword);
        user.setAccountActivated(true);
        user.setRoles(Set.of(testRole));
        userRepository.persist(user);
        userRepository.flush();
        this.currentUser = user;
    }

    void obtainToken() {
        bearerToken = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequestDTO(EMAIL, PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .path("token");
    }

    @Test
    @DisplayName("POST /conversations/add-message returns assistant reply with conversation ID when payload valid")
    void addMessage_shouldReturnAssistantReplyWithConversationId_whenContentValid() {
        when(chatCompletionService.generateReply(any(), any())).thenReturn("Bonjour, comment puis-je aider ?");

        given()
            .header("Authorization", "Bearer " + bearerToken)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"Bonjour\"}")
            .when()
            .post("/api/conversations/add-message")
            .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("role", equalTo(MessageRole.ASSISTANT.name()))
            .body("content", equalTo("Bonjour, comment puis-je aider ?"))
            .body("conversationId", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    @DisplayName("POST /conversations/add-message returns 400 when content blank")
    void addMessage_shouldReturnBadRequest_whenContentBlank() {
        given()
            .header("Authorization", "Bearer " + bearerToken)
            .contentType(ContentType.JSON)
            .body("{\"content\":\" \"}")
            .when()
            .post("/api/conversations/add-message")
            .then()
            .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
            .body(equalTo("content must be provided"));
    }

    @Test
    @DisplayName("GET /conversations/{id}/messages returns messages for authenticated user")
    void getConversationMessages_shouldReturnMessages_whenUserAuthorized() {
        Conversation conversation = conversationTestHelper.createPersistedConversation(currentUser.getId());

        when(chatCompletionService.generateReply(any(), any())).thenReturn("Salut !");

        given()
            .header("Authorization", "Bearer " + bearerToken)
            .when()
            .get("/api/conversations/" + conversation.getId() + "/messages")
            .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("", hasSize(0));

        given()
            .header("Authorization", "Bearer " + bearerToken)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"Salut\"}")
            .when()
            .post("/api/conversations/add-message")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());

        List<ChatMessage> messages = given()
            .header("Authorization", "Bearer " + bearerToken)
            .when()
            .get("/api/conversations/" + conversation.getId() + "/messages")
            .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .extract().as(new TypeRef<>() {});

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(0).content()).isEqualTo("Salut");
        assertThat(messages.get(1).role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(1).content()).isEqualTo("Salut !");
    }
}
