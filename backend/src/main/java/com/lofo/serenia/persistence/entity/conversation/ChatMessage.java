package com.lofo.serenia.persistence.entity.conversation;

/**
 * Minimal message view exchanged with LLM providers before encryption/persistence.
 */
public record ChatMessage(MessageRole role, String content) {
}
