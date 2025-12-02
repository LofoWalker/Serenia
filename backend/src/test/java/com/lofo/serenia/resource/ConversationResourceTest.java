package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.domain.conversation.MessageRole;
import com.lofo.serenia.domain.user.Role;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.dto.out.UserResponseDTO;
import com.lofo.serenia.repository.ConversationRepository;
import com.lofo.serenia.repository.RoleRepository;
import com.lofo.serenia.repository.UserRepository;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import com.lofo.serenia.service.token.TokenService;
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

    @InjectMock
    ChatCompletionService chatCompletionService;

    @Inject
    TokenService tokenService;

    @Inject
    UserRepository userRepository;

    @Inject
    UserTokenQuotaRepository userTokenQuotaRepository;

    @Inject
    UserTokenUsageRepository userTokenUsageRepository;

    @Inject
    ConversationRepository conversationRepository;
    @Inject
    RoleRepository roleRepository;
    @Inject
    ConversationTestHelper conversationTestHelper;

    private String bearerToken;
    private User currentUser;
    private Role testRole;

    @BeforeEach
    @Transactional
    void setUpIdentity() {
        userTokenUsageRepository.deleteAll();
        userTokenQuotaRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        roleRepository.deleteAll();
        testRole = Role.builder().name("USER").build();
        roleRepository.persist(testRole);

        User user = new User();
        user.setEmail(EMAIL);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("password");
        user.setRoles(Set.of(testRole));
        userRepository.persist(user);
        userRepository.flush();
        this.currentUser = user;

        UserResponseDTO userResponseDto = new UserResponseDTO(currentUser.getId(), "Doe", "John", EMAIL, Set.of("USER"));
        bearerToken = tokenService.generateToken(userResponseDto);
    }

    @Test
    @DisplayName("POST /conversations/add-message returns assistant reply when payload valid")
    void addMessage_shouldReturnAssistantReply_whenContentValid() {
        when(chatCompletionService.generateReply(any(), any())).thenReturn("Bonjour, comment puis-je aider ?");

        given()
            .auth().oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"Bonjour\"}")
            .when()
            .post("/api/conversations/add-message")
            .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("role", equalTo(MessageRole.ASSISTANT.name()))
            .body("content", equalTo("Bonjour, comment puis-je aider ?"));
    }

    @Test
    @DisplayName("POST /conversations/add-message returns 400 when content blank")
    void addMessage_shouldReturnBadRequest_whenContentBlank() {
        given()
            .auth().oauth2(bearerToken)
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
        // Create and persist conversation in a transactional context
        Conversation conversation = conversationTestHelper.createPersistedConversation(currentUser.getId());

        when(chatCompletionService.generateReply(any(), any())).thenReturn("Salut !");

        given()
            .auth().oauth2(bearerToken)
            .when()
            .get("/api/conversations/" + conversation.getId() + "/messages")
            .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .body("", hasSize(0)); // No messages persisted yet

        // Now, let's add a message and see if it's returned
        given()
            .auth().oauth2(bearerToken)
            .contentType(ContentType.JSON)
            .body("{\"content\":\"Salut\"}")
            .when()
            .post("/api/conversations/add-message")
            .then()
            .statusCode(Response.Status.OK.getStatusCode());

        List<ChatMessage> messages = given()
            .auth().oauth2(bearerToken)
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
