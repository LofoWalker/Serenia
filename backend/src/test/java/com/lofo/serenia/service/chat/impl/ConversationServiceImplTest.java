package com.lofo.serenia.service.chat.impl;

import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.domain.conversation.MessageRole;
import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.repository.ConversationRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConversationServiceImpl tests")
class ConversationServiceImplTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CONV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageService messageService;

    private ConversationServiceImpl conversationService;

    @BeforeEach
    void setup() {
        conversationService = new ConversationServiceImpl(conversationRepository, messageService);
        lenient().when(conversationRepository.findByIdAndUser(any(UUID.class), eq(FIXED_USER_ID)))
                .thenAnswer(invocation -> Optional.of(conversationWithId(invocation.getArgument(0), FIXED_USER_ID)));
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
    @DisplayName("Should create new conversation if none active")
    void should_create_new_conversation_if_none_active() {
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.empty());

        Conversation conv = conversationService.getOrCreateActiveConversation(FIXED_USER_ID);

        verify(conversationRepository).persist(any(Conversation.class));
    }

    private Conversation conversationWithId(UUID id, UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUserId(userId);
        return conversation;
    }
}
