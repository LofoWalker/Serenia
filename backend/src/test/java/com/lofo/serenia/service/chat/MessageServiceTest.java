package com.lofo.serenia.service.chat;

import com.lofo.serenia.mapper.MessageMapper;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService tests")
class MessageServiceTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CONV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageMapper messageMapper;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(messageRepository, encryptionService, conversationRepository, messageMapper);
    }

    @Test
    @DisplayName("Should persist user message with encrypted payload")
    void should_persist_user_message_with_encrypted_payload() {
        when(encryptionService.encryptForUser(FIXED_USER_ID, "Hello"))
                .thenReturn("[encrypted]Hello".getBytes());
        Conversation conversation = new Conversation();
        conversation.setId(FIXED_CONV_ID);
        when(conversationRepository.findByConversationId(FIXED_CONV_ID))
                .thenReturn(Optional.of(conversation));

        messageService.persistUserMessage(FIXED_USER_ID, FIXED_CONV_ID, "Hello");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).persist(captor.capture());
        Message persisted = captor.getValue();

        assertEquals(FIXED_CONV_ID, persisted.getConversationId());
        assertEquals(FIXED_USER_ID, persisted.getUserId());
        assertEquals(MessageRole.USER, persisted.getRole());
        assertArrayEquals("[encrypted]Hello".getBytes(), persisted.getEncryptedContent());
        assertNotNull(persisted.getTimestamp());
        assertNotNull(conversation.getLastActivityAt());
        assertTrue(conversation.getLastActivityAt().isAfter(Instant.EPOCH));
    }

    @Test
    @DisplayName("Should persist assistant message and return created entity")
    void should_persist_assistant_message_and_return_created_entity() {
        when(encryptionService.encryptForUser(FIXED_USER_ID, "Reply"))
                .thenReturn("[encrypted]Reply".getBytes());
        when(conversationRepository.findByConversationId(FIXED_CONV_ID))
                .thenReturn(Optional.empty());

        Message saved = messageService.persistAssistantMessage(FIXED_USER_ID, FIXED_CONV_ID, "Reply");

        assertEquals(FIXED_CONV_ID, saved.getConversationId());
        assertEquals(FIXED_USER_ID, saved.getUserId());
        assertEquals(MessageRole.ASSISTANT, saved.getRole());
        assertArrayEquals("[encrypted]Reply".getBytes(), saved.getEncryptedContent());
        assertNotNull(saved.getTimestamp());
        verify(messageRepository).persist(saved);
    }

    @Test
    @DisplayName("Should decrypt all conversation messages")
    void should_decrypt_all_conversation_messages() {
        Message message = new Message();
        message.setRole(MessageRole.ASSISTANT);
        message.setEncryptedContent("[encrypted]Answer".getBytes());
        when(messageRepository.findLatest(FIXED_CONV_ID)).thenReturn(List.of(message));
        when(encryptionService.decryptForUser(FIXED_USER_ID, message.getEncryptedContent()))
                .thenReturn("Answer");
        when(messageMapper.toChatMessage(message, "Answer"))
                .thenReturn(new ChatMessage(MessageRole.ASSISTANT, "Answer"));

        List<ChatMessage> decrypted = messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID);

        assertEquals(1, decrypted.size());
        assertEquals(MessageRole.ASSISTANT, decrypted.get(0).role());
        assertEquals("Answer", decrypted.get(0).content());
    }
}
