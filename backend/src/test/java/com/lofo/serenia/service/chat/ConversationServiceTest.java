package com.lofo.serenia.service.chat;

import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.service.user.shared.UserFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private MessageService messageService;

    @Mock
    private UserFinder userFinder;

    private ConversationService conversationService;

    @BeforeEach
    void setup() {
        conversationService = new ConversationService(conversationRepository, messageService, userFinder);
        lenient().when(conversationRepository.findByIdAndUser(any(UUID.class), eq(FIXED_USER_ID)))
                .thenAnswer(invocation -> Optional.of(conversationWithId(invocation.getArgument(0), FIXED_USER_ID)));
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
    @DisplayName("Should throw forbidden when user does not own conversation")
    void should_throw_forbidden_when_user_not_owner() {
        UUID convex = FIXED_CONV_ID;
        UUID otherUser = UUID.randomUUID();

        when(conversationRepository.findByIdAndUser(convex, otherUser)).thenReturn(Optional.empty());

        assertThrows(ForbiddenAccessException.class,
                () -> conversationService.getConversationMessages(convex, otherUser));
    }

    @Test
    @DisplayName("Should create new conversation with welcome message if none active")
    void should_create_new_conversation_if_none_active() {
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.empty());

        conversationService.getOrCreateActiveConversation(FIXED_USER_ID);

        verify(conversationRepository).persist(any(Conversation.class));
        verify(messageService).persistAssistantMessage(eq(FIXED_USER_ID), isNull(), contains(TEST_FIRST_NAME));
    }

    @Test
    @DisplayName("Should return active conversation when exists")
    void should_return_active_conversation_when_exists() {
        Conversation existingConversation = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.of(existingConversation));

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

    private Conversation conversationWithId(UUID id, UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUserId(userId);
        return conversation;
    }
}
