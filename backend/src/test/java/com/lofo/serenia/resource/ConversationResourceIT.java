package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.MessageRequestDTO;
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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for ConversationResource.
 * Tests cover authenticated message operations, conversation management, and access control.
 */
@QuarkusTest
@TestProfile(TestResourceProfile.class)
class ConversationResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    MessageRepository messageRepository;

    private static final String CONVERSATIONS_PATH = "/conversations";
    private static final String ADD_MESSAGE_PATH = CONVERSATIONS_PATH + "/add-message";
    private static final String MY_MESSAGES_PATH = CONVERSATIONS_PATH + "/my-messages";
    private static final String DELETE_CONVERSATIONS_PATH = CONVERSATIONS_PATH + "/my-conversations";
    private static final String GET_CONVERSATION_MESSAGES_PATH = CONVERSATIONS_PATH + "/{conversationId}/messages";

    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final String TEST_MESSAGE_CONTENT = "Hello, how can you help me?";

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ============== AUTHENTICATION TESTS ==============

    @Test
    @DisplayName("should_return_401_when_adding_message_without_auth")
    void should_return_401_when_adding_message_without_auth() {
        given()
                .contentType(ContentType.JSON)
                .body(new MessageRequestDTO(TEST_MESSAGE_CONTENT))
                .when()
                .post(ADD_MESSAGE_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_getting_my_messages_without_auth")
    void should_return_401_when_getting_my_messages_without_auth() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(MY_MESSAGES_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_deleting_conversations_without_auth")
    void should_return_401_when_deleting_conversations_without_auth() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete(DELETE_CONVERSATIONS_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_when_getting_conversation_messages_without_auth")
    void should_return_401_when_getting_conversation_messages_without_auth() {
        UUID conversationId = UUID.randomUUID();
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(GET_CONVERSATION_MESSAGES_PATH, conversationId)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should_return_401_with_invalid_jwt_token_when_adding_message")
    void should_return_401_with_invalid_jwt_token_when_adding_message() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer invalid-token")
                .body(new MessageRequestDTO(TEST_MESSAGE_CONTENT))
                .when()
                .post(ADD_MESSAGE_PATH)
                .then()
                .statusCode(401);
    }

    // ============== VALIDATION TESTS ==============

    @Test
    @DisplayName("should_return_400_when_adding_message_with_blank_content")
    @Transactional
    void should_return_400_when_adding_message_with_blank_content() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(new MessageRequestDTO(""))
                .when()
                .post(ADD_MESSAGE_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should_return_400_when_adding_message_with_null_content")
    @Transactional
    void should_return_400_when_adding_message_with_null_content() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"content\": null}")
                .when()
                .post(ADD_MESSAGE_PATH)
                .then()
                .statusCode(400);
    }

    // ============== GET USER MESSAGES TESTS ==============

    @Test
    @DisplayName("should_return_204_when_getting_messages_and_no_conversation_exists")
    @Transactional
    void should_return_204_when_getting_messages_and_no_conversation_exists() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(MY_MESSAGES_PATH)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("should_return_200_with_empty_messages_list_for_new_conversation")
    void should_return_200_with_empty_messages_list_for_new_conversation() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        // Create an empty conversation
        Conversation conversation = createConversation(user.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(MY_MESSAGES_PATH)
                .then()
                .statusCode(200)
                .body("conversationId", equalTo(conversation.getId().toString()))
                .body("messages", notNullValue());
    }

    // ============== DELETE CONVERSATIONS TESTS ==============

    @Test
    @DisplayName("should_return_204_when_deleting_user_conversations")
    @Transactional
    void should_return_204_when_deleting_user_conversations() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        // Create conversations to delete
        createConversation(user.getId());
        createConversation(user.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(DELETE_CONVERSATIONS_PATH)
                .then()
                .statusCode(204);

        // Verify conversations were deleted
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(MY_MESSAGES_PATH)
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("should_return_204_when_deleting_conversations_with_no_conversations")
    @Transactional
    void should_return_204_when_deleting_conversations_with_no_conversations() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(DELETE_CONVERSATIONS_PATH)
                .then()
                .statusCode(204);
    }

    // ============== ACCESS CONTROL TESTS ==============

    @Test
    @DisplayName("should_return_403_when_accessing_conversation_of_another_user")
    void should_return_403_when_accessing_conversation_of_another_user() {
        User user1 = createAndPersistUser(TEST_EMAIL);
        User user2 = createAndPersistUser("other@example.com");
        String user2Token = JwtTestTokenGenerator.generateToken("other@example.com", user2.getId(), "USER");

        Conversation conversation = createConversation(user1.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + user2Token)
                .when()
                .get(GET_CONVERSATION_MESSAGES_PATH, conversation.getId())
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("should_return_200_when_accessing_own_conversation_messages")
    void should_return_200_when_accessing_own_conversation_messages() {
        User user = createAndPersistUser(TEST_EMAIL);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, user.getId(), "USER");

        Conversation conversation = createConversation(user.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(GET_CONVERSATION_MESSAGES_PATH, conversation.getId())
                .then()
                .statusCode(200)
                .body("", notNullValue());
    }

    // ============== HELPER METHODS ==============

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
    Conversation createConversation(UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversationRepository.persistAndFlush(conversation);
        return conversation;
    }
}

