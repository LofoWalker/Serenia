package com.lofo.serenia.service.chat;

import com.lofo.serenia.exception.exceptions.ForbiddenAccessException;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.rest.dto.out.ConversationSummaryDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
@Transactional
public class ConversationService {

    private static final String DEFAULT_CONVERSATION_NAME = "Nouvelle conversation";
    private static final String WELCOME_MESSAGE_TEMPLATE =
        "Coucou %s ! C'est Serenia ✨ Ravi de te rencontrer. T'as passé une bonne journée ?";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final UserFinder userFinder;

    public Conversation getOrCreateActiveConversation(UUID userId, UUID conversationId) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        }
        return conversationRepository.findActiveByUser(userId)
            .orElseGet(() -> createConversation(userId, DEFAULT_CONVERSATION_NAME));
    }

    public List<ConversationSummaryDTO> listUserConversations(UUID userId) {
        return conversationRepository.findAllByUserOrderedByLastActivity(userId).stream()
            .map(c -> new ConversationSummaryDTO(c.getId(), c.getName(), c.getLastActivityAt()))
            .toList();
    }

    public Conversation createNewConversation(UUID userId, String name) {
        String conversationName = (name != null && !name.isBlank()) ? name : DEFAULT_CONVERSATION_NAME;
        return createConversation(userId, conversationName);
    }

    public Conversation renameConversation(UUID conversationId, UUID userId, String name) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        conversation.setName(name);
        conversationRepository.persist(conversation);
        return conversation;
    }

    public void deleteSingleConversation(UUID conversationId, UUID userId) {
        Conversation conversation = getOwnedConversation(conversationId, userId);
        messageRepository.delete("conversationId", conversation.getId());
        conversationRepository.delete(conversation);
    }

    public Conversation getMostRecentConversation(UUID userId) {
        return conversationRepository.findActiveByUser(userId).orElse(null);
    }

    public List<ChatMessage> getConversationMessages(UUID conversationId, UUID userId) {
        assertConversationOwnership(conversationId, userId);
        return messageService.decryptConversationMessages(userId, conversationId);
    }

    public Conversation getActiveConversationByUserId(UUID userId) {
        return conversationRepository.findActiveByUser(userId).orElse(null);
    }

    public void deleteUserConversations(UUID userId) {
        conversationRepository.deleteByUserId(userId);
    }

    private Conversation createConversation(UUID userId, String name) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setName(name);
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(Instant.now());
        conversationRepository.persist(conversation);

        String firstName = userFinder.findByIdOrThrow(userId).getFirstName();
        String welcomeMessage = String.format(WELCOME_MESSAGE_TEMPLATE, firstName);
        messageService.persistAssistantMessage(userId, conversation.getId(), welcomeMessage);

        return conversation;
    }

    private Conversation getOwnedConversation(UUID conversationId, UUID userId) {
        return conversationRepository.findByIdAndUser(conversationId, userId)
            .orElseThrow(() -> new ForbiddenAccessException("Conversation does not belong to user"));
    }

    private void assertConversationOwnership(UUID conversationId, UUID userId) {
        getOwnedConversation(conversationId, userId);
    }
}

