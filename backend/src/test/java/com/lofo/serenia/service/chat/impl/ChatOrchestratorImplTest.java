package com.lofo.serenia.service.chat.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.domain.conversation.Message;
import com.lofo.serenia.domain.conversation.MessageRole;
import com.lofo.serenia.service.chat.ChatCompletionService;
import com.lofo.serenia.service.chat.ConversationService;
import com.lofo.serenia.service.chat.ProcessedMessageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatOrchestratorImpl tests")
class ChatOrchestratorImplTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CONV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageService messageService;

    @Mock
    private ChatCompletionService chatCompletionService;

    @Mock
    private SereniaConfig sereniaConfig;

    private ChatOrchestratorImpl chatOrchestrator;

    @BeforeEach
    void setup() {
        chatOrchestrator = new ChatOrchestratorImpl(conversationService, messageService, chatCompletionService,
                sereniaConfig);
    }

    @Test
    @DisplayName("Should process user message and return assistant reply with conversation ID")
    void should_process_user_message_and_return_assistant_reply_with_conversation_id() {
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(messageService.persistAssistantMessage(eq(FIXED_USER_ID), eq(FIXED_CONV_ID), nullable(String.class)))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenReturn("Assistant reply");

        ProcessedMessageResult result = chatOrchestrator.processUserMessage(FIXED_USER_ID, "Hello world");

        assertNotNull(result);
        assertEquals(FIXED_CONV_ID, result.conversationId());
        assertNotNull(result.assistantMessage());
        assertEquals(MessageRole.ASSISTANT, result.assistantMessage().role());
        assertEquals("Assistant reply", result.assistantMessage().content());

        verify(messageService).persistUserMessage(FIXED_USER_ID, FIXED_CONV_ID, "Hello world");
        verify(messageService).persistAssistantMessage(FIXED_USER_ID, FIXED_CONV_ID, "Assistant reply");
        verify(chatCompletionService).generateReply(eq("System prompt"), anyList());
    }

    private Message messageWithRole(MessageRole role) {
        Message message = new Message();
        message.setRole(role);
        return message;
    }
}
