package com.lofo.serenia.service.chat;

import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;

import java.util.List;
import java.util.UUID;

public interface ConversationService {

    Conversation getOrCreateActiveConversation(UUID userId);

    List<ChatMessage> getConversationMessages(UUID conversationId, UUID userId);

}
