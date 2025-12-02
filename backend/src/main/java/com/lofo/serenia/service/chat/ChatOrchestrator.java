package com.lofo.serenia.service.chat;

import java.util.UUID;

public interface ChatOrchestrator {
    ProcessedMessageResult processUserMessage(UUID userId, String content);
}
