package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.service.subscription.QuotaService;
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
class ChatOrchestratorTest {

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

    @Mock
    private QuotaService quotaService;

    private ChatOrchestrator chatOrchestrator;

    @BeforeEach
    void setup() {
        chatOrchestrator = new ChatOrchestrator(conversationService, messageService, chatCompletionService,
                sereniaConfig, quotaService);
    }

    @Test
    @DisplayName("Should process user message and return assistant reply with conversation ID")
    void should_process_user_message_and_return_assistant_reply_with_conversation_id() {
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        ChatCompletionService.ChatCompletionResult completionResult =
                new ChatCompletionService.ChatCompletionResult("Assistant reply", 432);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(messageService.persistAssistantMessage(eq(FIXED_USER_ID), eq(FIXED_CONV_ID), nullable(String.class)))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenReturn(completionResult);

        ProcessedMessageResult result = chatOrchestrator.processUserMessage(FIXED_USER_ID, "Hello world");

        assertNotNull(result);
        assertEquals(FIXED_CONV_ID, result.conversationId());
        assertNotNull(result.assistantMessage());
        assertEquals(MessageRole.ASSISTANT, result.assistantMessage().role());
        assertEquals("Assistant reply", result.assistantMessage().content());

        verify(messageService).persistUserMessage(FIXED_USER_ID, FIXED_CONV_ID, "Hello world");
        verify(messageService).persistAssistantMessage(FIXED_USER_ID, FIXED_CONV_ID, "Assistant reply");
        verify(chatCompletionService).generateReply(eq("System prompt"), anyList());
        verify(quotaService).recordUsage(FIXED_USER_ID, 432);
    }

    @Test
    @DisplayName("Should propagate actual tokens from ChatCompletion to QuotaService")
    void should_propagate_actual_tokens_to_quota_service() {
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        int actualTokensUsed = 789;
        ChatCompletionService.ChatCompletionResult completionResult =
                new ChatCompletionService.ChatCompletionResult("Assistant response", actualTokensUsed);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(messageService.persistAssistantMessage(eq(FIXED_USER_ID), eq(FIXED_CONV_ID), nullable(String.class)))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenReturn(completionResult);

        ProcessedMessageResult result = chatOrchestrator.processUserMessage(FIXED_USER_ID, "Test message");

        assertNotNull(result);
        verify(quotaService).recordUsage(FIXED_USER_ID, actualTokensUsed);
    }

    private Message messageWithRole(MessageRole role) {
        Message message = new Message();
        message.setRole(role);
        return message;
    }
}
