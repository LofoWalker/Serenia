package com.lofo.serenia.mapper;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MessageMapper unit tests")
class MessageMapperTest {

    private final MessageMapper messageMapper = new MessageMapper() {
    };

    @Test
    @DisplayName("should_map_message_to_chat_message_with_decrypted_content")
    void toChatMessageShouldMapMessageToChatMessageWithDecryptedContent() {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setRole(MessageRole.USER);
        message.setEncryptedContent("encrypted".getBytes());
        message.setTimestamp(Instant.now());

        String decryptedContent = "Hello World";

        ChatMessage result = messageMapper.toChatMessage(message, decryptedContent);

        assertNotNull(result);
        assertEquals(MessageRole.USER, result.role());
        assertEquals(decryptedContent, result.content());
    }

    @Test
    @DisplayName("should_map_assistant_message_to_chat_message")
    void toChatMessageShouldMapAssistantMessageToChatMessage() {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setRole(MessageRole.ASSISTANT);
        message.setEncryptedContent("encrypted".getBytes());
        message.setTimestamp(Instant.now());

        String decryptedContent = "Response from assistant";

        ChatMessage result = messageMapper.toChatMessage(message, decryptedContent);

        assertNotNull(result);
        assertEquals(MessageRole.ASSISTANT, result.role());
        assertEquals(decryptedContent, result.content());
    }

    @Test
    @DisplayName("should_return_null_when_toChatMessage_receives_null_message")
    void toChatMessageShouldReturnNullWhenMessageIsNull() {
        ChatMessage result = messageMapper.toChatMessage(null, "content");

        assertNull(result);
    }
}

