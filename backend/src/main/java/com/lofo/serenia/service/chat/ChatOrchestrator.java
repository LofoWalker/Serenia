package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.service.subscription.QuotaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@ApplicationScoped
public class ChatOrchestrator {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatCompletionService chatCompletionService;
    private final SereniaConfig sereniaConfig;
    private final QuotaService quotaService;

    @Transactional
    public ProcessedMessageResult processUserMessage(UUID userId, String content) {
        Conversation conv = conversationService.getOrCreateActiveConversation(userId);

        quotaService.checkQuotaBeforeCall(userId);

        messageService.persistUserMessage(userId, conv.getId(), content);

        List<ChatMessage> history = messageService.decryptConversationMessages(userId, conv.getId());
        String assistantReply = chatCompletionService.generateReply(
                sereniaConfig.systemPrompt(),
                history
        );

        Message assistantMsg = messageService.persistAssistantMessage(userId, conv.getId(), assistantReply);

        quotaService.recordUsage(userId, content, assistantReply);

        ChatMessage chatMessage = new ChatMessage(assistantMsg.getRole(), assistantReply);

        return new ProcessedMessageResult(conv.getId(), chatMessage);
    }
}

