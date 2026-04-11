package com.lofo.serenia.service.chat;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.rest.dto.out.ConversationSummaryDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConversationService tests")
class ConversationServiceTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CONV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String TEST_FIRST_NAME = "Tom";

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private UserFinder userFinder;

    private ConversationService conversationService;

    @BeforeEach
    void setup() {
        conversationService = new ConversationService(
            conversationRepository, messageRepository, messageService, userFinder);
        lenient().when(conversationRepository.findByIdAndUser(any(UUID.class), eq(FIXED_USER_ID)))
            .thenAnswer(invocation -> Optional.of(
                conversationWithId(invocation.getArgument(0), FIXED_USER_ID)));
        lenient().when(userFinder.findByIdOrThrow(FIXED_USER_ID))
            .thenReturn(User.builder().id(FIXED_USER_ID).firstName(TEST_FIRST_NAME).build());
    }

    @Test
    @DisplayName("Should decrypt conversation messages")
    void should_decrypt_messages_when_requested() {
        when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, FIXED_USER_ID))
            .thenReturn(Optional.of(conversationWithId(FIXED_CONV_ID, FIXED_USER_ID)));
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
            .thenReturn(Collections.singletonList(new ChatMessage(MessageRole.USER, "Hi")));

        List<ChatMessage> decrypted = conversationService.getConversationMessages(FIXED_CONV_ID, FIXED_USER_ID);

        assertEquals(1, decrypted.size());
        assertEquals("Hi", decrypted.get(0).content());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not own conversation")
    void should_throw_not_found_when_user_not_owner() {
        UUID otherUser = UUID.randomUUID();
        when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, otherUser)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> conversationService.getConversationMessages(FIXED_CONV_ID, otherUser));
    }

    @Test
    @DisplayName("Should create new conversation with welcome message if none active")
    void should_create_new_conversation_if_none_active() {
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.empty());

        conversationService.getOrCreateActiveConversation(FIXED_USER_ID, null);

        verify(conversationRepository).persist(any(Conversation.class));
        verify(messageService).persistAssistantMessage(eq(FIXED_USER_ID), any(), contains(TEST_FIRST_NAME));
    }

    @Test
    @DisplayName("Should return active conversation when exists")
    void should_return_active_conversation_when_exists() {
        Conversation existingConversation = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findActiveByUser(FIXED_USER_ID))
            .thenReturn(Optional.of(existingConversation));

        Conversation result = conversationService.getActiveConversationByUserId(FIXED_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(FIXED_CONV_ID);
        assertThat(result.getUserId()).isEqualTo(FIXED_USER_ID);
    }

    @Test
    @DisplayName("Should return null when no active conversation exists")
    void should_return_null_when_no_active_conversation() {
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.empty());

        Conversation result = conversationService.getActiveConversationByUserId(FIXED_USER_ID);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should use specified conversationId when provided")
    void should_use_specified_conversation_id_when_provided() {
        Conversation existing = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, FIXED_USER_ID))
            .thenReturn(Optional.of(existing));

        Conversation result = conversationService.getOrCreateActiveConversation(FIXED_USER_ID, FIXED_CONV_ID);

        assertThat(result.getId()).isEqualTo(FIXED_CONV_ID);
        verify(conversationRepository, never()).findActiveByUser(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when specified conversationId not found")
    void should_throw_not_found_when_conversation_id_invalid() {
        UUID invalidId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUser(invalidId, FIXED_USER_ID))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> conversationService.getOrCreateActiveConversation(FIXED_USER_ID, invalidId));
    }

    @Test
    @DisplayName("Should list user conversations")
    void should_list_user_conversations() {
        Conversation c1 = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        c1.setName("Conv 1");
        c1.setLastActivityAt(Instant.now());
        Conversation c2 = conversationWithId(UUID.randomUUID(), FIXED_USER_ID);
        c2.setName("Conv 2");
        c2.setLastActivityAt(Instant.now());

        when(conversationRepository.findAllByUserOrderedByLastActivity(FIXED_USER_ID))
            .thenReturn(List.of(c1, c2));

        List<ConversationSummaryDTO> result = conversationService.listUserConversations(FIXED_USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Conv 1");
    }

    @Test
    @DisplayName("Should create new conversation with custom name")
    void should_create_new_conversation_with_custom_name() {
        conversationService.createNewConversation(FIXED_USER_ID, "Ma discussion");

        verify(conversationRepository).persist(argThat((Conversation c) ->
            "Ma discussion".equals(c.getName())));
    }

    @Test
    @DisplayName("Should create new conversation with default name when blank")
    void should_create_new_conversation_with_default_name_when_blank() {
        conversationService.createNewConversation(FIXED_USER_ID, "");

        verify(conversationRepository).persist(argThat((Conversation c) ->
            "Nouvelle conversation".equals(c.getName())));
    }

    @Test
    @DisplayName("Should rename conversation")
    void should_rename_conversation() {
        Conversation conversation = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, FIXED_USER_ID))
            .thenReturn(Optional.of(conversation));

        Conversation result = conversationService.renameConversation(FIXED_CONV_ID, FIXED_USER_ID, "New Name");

        assertThat(result.getName()).isEqualTo("New Name");
        verify(conversationRepository).persist(conversation);
    }

    @Test
    @DisplayName("Should delete single conversation with its messages")
    void should_delete_single_conversation() {
        Conversation conversation = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, FIXED_USER_ID))
            .thenReturn(Optional.of(conversation));

        conversationService.deleteSingleConversation(FIXED_CONV_ID, FIXED_USER_ID);

        verify(messageRepository).delete("conversationId", FIXED_CONV_ID);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-owned conversation")
    void should_throw_not_found_when_deleting_non_owned_conversation() {
        UUID otherUser = UUID.randomUUID();
        when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, otherUser))
            .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> conversationService.deleteSingleConversation(FIXED_CONV_ID, otherUser));
    }

    @Test
    @DisplayName("Should get most recent conversation")
    void should_get_most_recent_conversation() {
        Conversation conv = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findActiveByUser(FIXED_USER_ID))
            .thenReturn(Optional.of(conv));

        Conversation result = conversationService.getMostRecentConversation(FIXED_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(FIXED_CONV_ID);
    }

    private Conversation conversationWithId(UUID id, UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUserId(userId);
        return conversation;
    }
}
