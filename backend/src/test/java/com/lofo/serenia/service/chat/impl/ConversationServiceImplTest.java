package com.lofo.serenia.service.chat.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.domain.conversation.Message;
import com.lofo.serenia.domain.conversation.MessageRole;
import com.lofo.serenia.exception.ForbiddenAccessException;
import com.lofo.serenia.repository.ConversationRepository;
import com.lofo.serenia.service.chat.ChatCompletionService;
import com.lofo.serenia.service.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyList;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConversationServiceImpl tests")
class ConversationServiceImplTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CONV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ChatCompletionService chatCompletionService;

    @Mock
    private MessageService messageService;

    @Mock
    private SereniaConfig sereniaConfig;

    private ConversationServiceImpl conversationService;

    @BeforeEach
    void setup() {

        conversationService = new ConversationServiceImpl(conversationRepository, encryptionService,
                chatCompletionService, messageService, sereniaConfig);
        stubEncryptionKeyCreation();
        stubAiReply();
        lenient().when(conversationRepository.findByIdAndUser(any(UUID.class), eq(FIXED_USER_ID)))
                .thenAnswer(invocation -> Optional.of(conversationWithId(invocation.getArgument(0), FIXED_USER_ID)));
    }

    @Test
    @DisplayName("Should add user message and return assistant reply")
    void should_add_user_message_and_return_assistant_reply() {
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.empty());
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(messageService.persistAssistantMessage(eq(FIXED_USER_ID), eq(FIXED_CONV_ID), nullable(String.class)))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));

        doAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            conv.setId(FIXED_CONV_ID);
            when(conversationRepository.findByConversationId(FIXED_CONV_ID)).thenReturn(Optional.of(conv));
            when(conversationRepository.findByIdAndUser(FIXED_CONV_ID, FIXED_USER_ID))
                    .thenReturn(Optional.of(conv));
            return null;
        }).when(conversationRepository).persist(any(Conversation.class));

        ChatMessage assistantMsg = conversationService.addUserMessage(FIXED_USER_ID, "Hello world");

        assertNotNull(assistantMsg);
        assertEquals(MessageRole.ASSISTANT, assistantMsg.role());
        assertEquals("Assistant reply", assistantMsg.content());

        verify(messageService).persistUserMessage(FIXED_USER_ID, FIXED_CONV_ID, "Hello world");
        verify(messageService).persistAssistantMessage(FIXED_USER_ID, FIXED_CONV_ID, "Assistant reply");
        verify(chatCompletionService).generateReply("System prompt", Collections.emptyList());
        verify(conversationRepository).persist(any(Conversation.class));
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
    @DisplayName("Should reuse existing active conversation when adding message")
    void should_reuse_existing_active_conversation() {
        Conversation conv = conversationWithId(FIXED_CONV_ID, FIXED_USER_ID);
        when(conversationRepository.findActiveByUser(FIXED_USER_ID)).thenReturn(Optional.of(conv));
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(new ArrayList<>());
        when(messageService.persistAssistantMessage(eq(FIXED_USER_ID), eq(FIXED_CONV_ID), nullable(String.class)))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));

        ChatMessage assistantMsg = conversationService.addUserMessage(FIXED_USER_ID, "Hi again");

        assertEquals(MessageRole.ASSISTANT, assistantMsg.role());
        verify(messageService).persistUserMessage(FIXED_USER_ID, FIXED_CONV_ID, "Hi again");
        verify(messageService).persistAssistantMessage(FIXED_USER_ID, FIXED_CONV_ID, "Assistant reply");
        verify(conversationRepository, never()).persist(any(Conversation.class));
    }

    private void stubEncryptionKeyCreation() {
        doNothing().when(encryptionService).createUserKeyIfAbsent(FIXED_USER_ID);
    }

    private void stubAiReply() {
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenReturn("Assistant reply");
    }

    private Conversation conversationWithId(UUID id, UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUserId(userId);
        return conversation;
    }

    private Message messageWithRole(MessageRole role) {
        Message message = new Message();
        message.setRole(role);
        return message;
    }
}
