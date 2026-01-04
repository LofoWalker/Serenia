package com.lofo.serenia.service.chat;

import com.lofo.serenia.mapper.MessageMapper;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
public class MessageService {

    private final MessageRepository messageRepository;
    private final EncryptionService encryptionService;
    private final ConversationRepository conversationRepository;
    private final MessageMapper messageMapper;

    @Transactional
    public void persistUserMessage(UUID userId, UUID conversationId, String content) {
        persistMessage(userId, conversationId, MessageRole.USER, content);
    }

    @Transactional
    public Message persistAssistantMessage(UUID userId, UUID conversationId, String assistantReply) {
        return persistMessage(userId, conversationId, MessageRole.ASSISTANT, assistantReply);
    }

    public List<ChatMessage> decryptConversationMessages(UUID userId, UUID conversationId) {
        List<Message> messages = messageRepository.findByConversation(conversationId);
        return messages.stream()
                .map(message -> messageMapper.toChatMessage(message,
                        encryptionService.decryptForUser(userId, message.getEncryptedContent())))
                .toList();
    }

    private Message persistMessage(UUID userId, UUID conversationId, MessageRole role, String content) {
        byte[] encryptedContent = encryptionService.encryptForUser(userId, content);
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setEncryptedContent(encryptedContent);
        message.setTimestamp(Instant.now());
        messageRepository.persist(message);
        refreshConversationActivity(conversationId);
        return message;
    }

    private void refreshConversationActivity(UUID conversationId) {
        conversationRepository.findByConversationId(conversationId).ifPresent(conversation -> {
            conversation.setLastActivityAt(Instant.now());
            conversationRepository.persist(conversation);
        });
    }
}

