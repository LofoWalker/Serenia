package com.lofo.serenia.rest.dto.out;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;

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
