package com.lofo.serenia.resource;

import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.repository.ConversationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ConversationTestHelper {

    @Inject
    ConversationRepository conversationRepository;

    @Transactional
    public Conversation createPersistedConversation(UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(Instant.now());
        conversationRepository.persist(conversation);
        conversationRepository.flush();
        return conversation;
    }
}

