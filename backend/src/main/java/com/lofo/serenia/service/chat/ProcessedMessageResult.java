package com.lofo.serenia.service.chat;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;

import java.util.UUID;

/**
 * DTO returned by ChatOrchestrator containing the assistant message
 * and the conversation ID.
 */
public record ProcessedMessageResult(
        UUID conversationId,
        ChatMessage assistantMessage
) {
}

