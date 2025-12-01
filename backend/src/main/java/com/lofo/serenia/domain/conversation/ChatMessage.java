package com.lofo.serenia.domain.conversation;

/**
 * Minimal message view exchanged with LLM providers before encryption/persistence.
 */
public record ChatMessage(MessageRole role, String content) {
}
