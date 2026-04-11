package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.conversation.Conversation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationRepository implements PanacheRepository<Conversation> {

    public Optional<Conversation> findActiveByUser(UUID userId) {
        return find("userId = ?1 ORDER BY lastActivityAt DESC", userId).firstResultOptional();
    }

    public List<Conversation> findAllByUserOrderedByLastActivity(UUID userId) {
        return find("userId = ?1 ORDER BY lastActivityAt DESC", userId).list();
    }

    public Optional<Conversation> findByConversationId(UUID conversationId) {
        return find("id = ?1", conversationId).firstResultOptional();
    }

    public Optional<Conversation> findByIdAndUser(UUID conversationId, UUID userId) {
        return find("id = ?1 and userId = ?2", conversationId, userId).firstResultOptional();
    }

    public void deleteByUserId(UUID userId) {
        delete("userId", userId);
    }
}
