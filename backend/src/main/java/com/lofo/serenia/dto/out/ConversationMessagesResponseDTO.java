package com.lofo.serenia.dto.out;

import com.lofo.serenia.domain.conversation.ChatMessage;

import java.util.List;
import java.util.UUID;

public record ConversationMessagesResponseDTO(
        UUID conversationId,
        List<ChatMessage> messages
) {
}

