package com.lofo.serenia.service.chat;

import com.lofo.serenia.domain.conversation.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface ConversationService {

    ChatMessage addUserMessage(UUID userId, String content);

    List<ChatMessage> getConversationMessages(UUID conversationId, UUID userId);

}
