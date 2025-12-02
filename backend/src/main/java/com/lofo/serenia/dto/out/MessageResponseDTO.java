package com.lofo.serenia.dto.out;

import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.MessageRole;

import java.util.UUID;

/**
 * Response DTO for the add-message endpoint.
 * Contains the assistant's reply and the conversation ID.
 */
public record MessageResponseDTO(
        UUID conversationId,
        MessageRole role,
        String content
) {
    /**
     * Create a MessageResponseDTO from a ChatMessage and conversationId
     */
    public static MessageResponseDTO from(UUID conversationId, ChatMessage chatMessage) {
        return new MessageResponseDTO(
                conversationId,
                chatMessage.role(),
                chatMessage.content()
        );
    }
}

