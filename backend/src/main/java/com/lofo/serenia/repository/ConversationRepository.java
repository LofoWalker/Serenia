package com.lofo.serenia.repository;

import com.lofo.serenia.domain.conversation.Conversation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationRepository implements PanacheRepository<Conversation> {

    public Optional<Conversation> findActiveByUser(UUID userId) {
        return find("userId = ?1", userId).firstResultOptional();
    }

    public Optional<Conversation> findByConversationId(UUID conversationId) {
        return find("id = ?1", conversationId).firstResultOptional();
    }

    /**
     * Ensures the returned conversation belongs to the requesting user.
     */
    public Optional<Conversation> findByIdAndUser(UUID conversationId, UUID userId) {
        return find("id = ?1 and userId = ?2", conversationId, userId).firstResultOptional();
    }

    public void deleteByUserId(UUID userId) {
        delete("userId", userId);
    }
}
