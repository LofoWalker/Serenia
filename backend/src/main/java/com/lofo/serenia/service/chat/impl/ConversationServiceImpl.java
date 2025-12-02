package com.lofo.serenia.service.chat.impl;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Conversation;
import com.lofo.serenia.domain.conversation.Message;
import com.lofo.serenia.exception.ForbiddenAccessException;
import com.lofo.serenia.repository.ConversationRepository;
import com.lofo.serenia.service.chat.ChatCompletionService;
import com.lofo.serenia.service.chat.ConversationService;
import com.lofo.serenia.service.encryption.EncryptionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final EncryptionService encryptionService;
    private final ChatCompletionService chatCompletionService;
    private final MessageService messageService;
    private final SereniaConfig sereniaConfig;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   EncryptionService encryptionService,
                                   ChatCompletionService chatCompletionService,
                                   MessageService messageService,
                                   SereniaConfig sereniaConfig) {
        this.conversationRepository = conversationRepository;
        this.encryptionService = encryptionService;
        this.chatCompletionService = chatCompletionService;
        this.messageService = messageService;
        this.sereniaConfig = sereniaConfig;
    }

    @Override
    public ChatMessage addUserMessage(UUID userId, String content) {
        Conversation conv = getOrCreateActiveConversation(userId);
        messageService.persistUserMessage(userId, conv.getId(), content);
        String assistantReply = chatCompletionService.generateReply(sereniaConfig.systemPrompt(),
                messageService.decryptConversationMessages(userId, conv.getId()));
        Message assistantMsg = messageService.persistAssistantMessage(userId, conv.getId(), assistantReply);
        return new ChatMessage(assistantMsg.getRole(), assistantReply);
    }

    @Override
    public List<ChatMessage> getConversationMessages(UUID conversationId, UUID userId) {
        return getDecryptedMessagesForConversation(userId, conversationId);
    }

    private Conversation getOrCreateActiveConversation(UUID userId) {
        return conversationRepository.findActiveByUser(userId)
                .orElseGet(() -> startConversation(userId));
    }

    private Conversation startConversation(UUID userId) {
        encryptionService.createUserKeyIfAbsent(userId);

        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setCreatedAt(Instant.now());
        conversation.setLastActivityAt(Instant.now());

        conversationRepository.persist(conversation);
        return conversation;
    }

    private Conversation assertConversationOwnership(UUID conversationId, UUID userId) {
        return conversationRepository.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new ForbiddenAccessException("Conversation does not belong to user"));
    }

    private List<ChatMessage> getDecryptedMessagesForConversation(UUID userId, UUID conversationId) {
        assertConversationOwnership(conversationId, userId);
        return messageService.decryptConversationMessages(userId, conversationId);
    }
}
