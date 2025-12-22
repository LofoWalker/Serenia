package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.conversation.Message;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {

    public List<Message> findByConversation(UUID conversationId) {
        return list("conversationId = ?1 ORDER BY timestamp ASC", conversationId);
    }

    public void deleteByUserId(UUID userId) {
        delete("userId", userId);
    }
}
