package com.lofo.serenia.persistence.entity.conversation;

import java.time.Instant;

/**
 * Minimal message view exchanged with LLM providers before encryption/persistence.
 */
public record ChatMessage(MessageRole role, String content, Instant timestamp) {

    /**
     * Constructor without timestamp for LLM provider compatibility.
     */
    public ChatMessage(MessageRole role, String content) {
        this(role, content, null);
    }
}
