package com.lofo.serenia.service.user;
import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.UserResponseDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@QuarkusTest
@TestProfile(TestResourceProfile.class)
class AccountManagementServiceIT {
    @Inject
    AccountManagementService accountManagementService;
    @Inject
    UserRepository userRepository;
    @Inject
    ConversationRepository conversationRepository;
    @Inject
    MessageRepository messageRepository;
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    @BeforeEach
    @Transactional
    void setup() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }
    @Test
    @DisplayName("should_retrieve_user_profile_when_user_exists")
    void should_retrieve_user_profile_when_user_exists() {
        createUser(TEST_EMAIL);
        UserResponseDTO profile = accountManagementService.getUserProfile(TEST_EMAIL);
        assertThat(profile).isNotNull();
        assertThat(profile.email()).isEqualTo(TEST_EMAIL);
        assertThat(profile.firstName()).isEqualTo(TEST_FIRST_NAME);
        assertThat(profile.lastName()).isEqualTo(TEST_LAST_NAME);
        assertThat(profile.role()).isEqualTo("USER");
    }
    @Test
    @DisplayName("should_throw_not_found_when_getting_profile_for_nonexistent_user")
    void should_throw_not_found_when_getting_profile_for_nonexistent_user() {
        assertThatThrownBy(() -> accountManagementService.getUserProfile("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }
    @Test
    @DisplayName("should_delete_user_account_when_exists")
    void should_delete_user_account_when_exists() {
        createUser(TEST_EMAIL);
        assertThat(userRepository.count()).isEqualTo(1);
        accountManagementService.deleteAccountAndAssociatedData(TEST_EMAIL);
        assertThat(userRepository.count()).isZero();
    }
    @Test
    @DisplayName("should_throw_not_found_when_deleting_nonexistent_user")
    void should_throw_not_found_when_deleting_nonexistent_user() {
        assertThatThrownBy(() -> accountManagementService.deleteAccountAndAssociatedData("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }
    @Test
    @DisplayName("should_delete_user_conversations_when_deleting_account")
    @Transactional
    void should_delete_user_conversations_when_deleting_account() {
        User user = createAndReturnUser(TEST_EMAIL);
        createConversation(user.getId());
        createConversation(user.getId());
        assertThat(conversationRepository.count()).isEqualTo(2);
        accountManagementService.deleteAccountAndAssociatedData(TEST_EMAIL);
        assertThat(conversationRepository.count()).isZero();
    }
    @Test
    @DisplayName("should_delete_user_messages_when_deleting_account")
    @Transactional
    void should_delete_user_messages_when_deleting_account() {
        User user = createAndReturnUser(TEST_EMAIL);
        Conversation conversation = createConversation(user.getId());
        createMessage(user.getId(), conversation.getId());
        createMessage(user.getId(), conversation.getId());
        assertThat(messageRepository.count()).isEqualTo(2);
        accountManagementService.deleteAccountAndAssociatedData(TEST_EMAIL);
        assertThat(messageRepository.count()).isZero();
    }
    @Test
    @DisplayName("should_not_delete_other_users_data_when_deleting_account")
    @Transactional
    void should_not_delete_other_users_data_when_deleting_account() {
        User user1 = createAndReturnUser(TEST_EMAIL);
        User user2 = createAndReturnUser("other@example.com");
        Conversation conv1 = createConversation(user1.getId());
        Conversation conv2 = createConversation(user2.getId());
        createMessage(user1.getId(), conv1.getId());
        createMessage(user2.getId(), conv2.getId());
        assertThat(conversationRepository.count()).isEqualTo(2);
        assertThat(messageRepository.count()).isEqualTo(2);
        accountManagementService.deleteAccountAndAssociatedData(TEST_EMAIL);
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(messageRepository.count()).isEqualTo(1);
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
    @Transactional
    User createAndReturnUser(String email) {
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
    @Transactional
    void createMessage(UUID userId, UUID conversationId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setConversationId(conversationId);
        message.setRole(MessageRole.USER);
        message.setEncryptedContent("test".getBytes());
        message.setTimestamp(Instant.now());
        messageRepository.persistAndFlush(message);
    }
}
