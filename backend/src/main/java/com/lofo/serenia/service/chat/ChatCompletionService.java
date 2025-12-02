package com.lofo.serenia.service.chat;

import com.lofo.serenia.domain.conversation.ChatMessage;

import java.util.List;

public interface ChatCompletionService {

    /**
     * Generates an assistant reply using the system prompt plus decrypted conversation history.
     */
    String generateReply(String systemPrompt, List<ChatMessage> conversationMessages);
}
