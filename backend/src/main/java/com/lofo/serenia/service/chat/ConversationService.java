package com.lofo.serenia.service.chat;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageService messageService;

    public Conversation getOrCreateActiveConversation(UUID userId) {
        return conversationRepository.findActiveByUser(userId)
                .orElseGet(() -> startConversation(userId));
    }

    public List<ChatMessage> getConversationMessages(UUID conversationId, UUID userId) {
        return getDecryptedMessagesForConversation(userId, conversationId);
    }

    public Conversation getActiveConversationByUserId(UUID userId) {
        return conversationRepository.findActiveByUser(userId).orElse(null);
    }

    public void deleteUserConversations(UUID userId) {
        conversationRepository.deleteByUserId(userId);
    }

    private Conversation startConversation(UUID userId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(Instant.now());

        conversationRepository.persist(conversation);
        return conversation;
    }

    private void assertConversationOwnership(UUID conversationId, UUID userId) {
        conversationRepository.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new ForbiddenAccessException("Conversation does not belong to user"));
    }

    private List<ChatMessage> getDecryptedMessagesForConversation(UUID userId, UUID conversationId) {
        assertConversationOwnership(conversationId, userId);
        return messageService.decryptConversationMessages(userId, conversationId);
    }
}

