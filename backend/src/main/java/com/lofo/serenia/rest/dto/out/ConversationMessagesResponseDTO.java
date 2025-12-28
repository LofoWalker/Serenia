package com.lofo.serenia.rest.dto.out;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;

import java.util.List;
import java.util.UUID;

public record ConversationMessagesResponseDTO(
        UUID conversationId,
        List<ChatMessage> messages
) {
}

