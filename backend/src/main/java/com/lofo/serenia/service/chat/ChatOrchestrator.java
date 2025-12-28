package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
public class ChatOrchestrator {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatCompletionService chatCompletionService;
    private final SereniaConfig sereniaConfig;

    @Transactional
    public ProcessedMessageResult processUserMessage(UUID userId, String content) {
        Conversation conv = conversationService.getOrCreateActiveConversation(userId);
        messageService.persistUserMessage(userId, conv.getId(), content);

        String assistantReply = chatCompletionService.generateReply(sereniaConfig.systemPrompt(),
                messageService.decryptConversationMessages(userId, conv.getId()));

        Message assistantMsg = messageService.persistAssistantMessage(userId, conv.getId(), assistantReply);
        ChatMessage chatMessage = new ChatMessage(assistantMsg.getRole(), assistantReply);

        return new ProcessedMessageResult(conv.getId(), chatMessage);
    }
}

