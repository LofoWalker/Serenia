package com.lofo.serenia.dto.out;

import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.MessageRole;

import java.util.UUID;

/**
 * Response DTO for chat messages.
 * Contains conversation context and message details.
 */
public record MessageResponseDTO(
        UUID conversationId,
        MessageRole role,
        String content
) {
    public static MessageResponseDTO from(UUID conversationId, ChatMessage message) {
        return new MessageResponseDTO(conversationId, message.role(), message.content());
    }
}
