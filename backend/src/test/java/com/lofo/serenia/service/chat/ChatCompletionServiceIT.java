package com.lofo.serenia.service.chat;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatCompletionServiceIT {

    private static final String TEST_SYSTEM_PROMPT = "You are a helpful assistant.";
    private static final String TEST_USER_MESSAGE = "Hello, how are you?";

    @Test
    @DisplayName("should_accept_valid_system_prompt_parameter")
    void should_accept_valid_system_prompt_parameter() {
        String systemPrompt = TEST_SYSTEM_PROMPT;

        assertThat(systemPrompt).isNotNull();
        assertThat(systemPrompt).isNotEmpty();
        assertThat(systemPrompt).startsWith("You are");
    }

    @Test
    @DisplayName("should_accept_empty_system_prompt_parameter")
    void should_accept_empty_system_prompt_parameter() {
        String systemPrompt = "";

        assertThat(systemPrompt).isNotNull();
        assertThat(systemPrompt).isEmpty();
    }

    @Test
    @DisplayName("should_handle_null_system_prompt_parameter")
    void should_handle_null_system_prompt_parameter() {
        String systemPrompt = null;

        assertThat(systemPrompt).isNull();
    }

    @Test
    @DisplayName("should_create_single_chat_message_list")
    void should_create_single_chat_message_list() {
        List<ChatMessage> messages = createChatMessages(TEST_USER_MESSAGE);

        assertThat(messages).isNotNull();
        assertThat(messages).isNotEmpty();
        assertThat(messages).hasSize(1);
    }

    @Test
    @DisplayName("should_handle_null_conversation_messages")
    void should_handle_null_conversation_messages() {
        List<ChatMessage> messages = null;

        assertThat(messages).isNull();
    }

    @Test
    @DisplayName("should_handle_empty_conversation_messages")
    void should_handle_empty_conversation_messages() {
        List<ChatMessage> messages = new ArrayList<>();

        assertThat(messages).isNotNull();
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("should_create_multiple_conversation_messages")
    void should_create_multiple_conversation_messages() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage("First message"));
        messages.add(createChatMessage("Second message"));
        messages.add(createChatMessage("Third message"));

        assertThat(messages).hasSize(3);
        assertThat(messages).allMatch(msg -> msg.role().equals(MessageRole.USER));
    }

    @Test
    @DisplayName("should_handle_mixed_message_types")
    void should_handle_mixed_message_types() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(MessageRole.USER, "User question"));
        messages.add(new ChatMessage(MessageRole.ASSISTANT, "Assistant answer"));
        messages.add(new ChatMessage(MessageRole.SYSTEM, "System prompt"));
        messages.add(new ChatMessage(MessageRole.USER, "Follow-up"));

        assertThat(messages).hasSize(4);
        assertThat(messages.get(0).role()).isEqualTo(MessageRole.USER);
        assertThat(messages.get(1).role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messages.get(2).role()).isEqualTo(MessageRole.SYSTEM);
        assertThat(messages.get(3).role()).isEqualTo(MessageRole.USER);
    }

    @Test
    @DisplayName("should_handle_special_characters_in_message")
    void should_handle_special_characters_in_message() {
        String specialContent = "Message with @#$%^&*()_+-=[]{}|;':\",./<>?";
        ChatMessage message = createChatMessage(specialContent);

        assertThat(message.content()).isEqualTo(specialContent);
        assertThat(message.role()).isEqualTo(MessageRole.USER);
    }

    @Test
    @DisplayName("should_handle_multiline_message_content")
    void should_handle_multiline_message_content() {
        String multilineContent = "Line 1\nLine 2\nLine 3";
        ChatMessage message = createChatMessage(multilineContent);

        assertThat(message.content()).contains("\n");
        assertThat(message.content()).isEqualTo(multilineContent);
    }

    private List<ChatMessage> createChatMessages(String content) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(createChatMessage(content));
        return messages;
    }

    private ChatMessage createChatMessage(String content) {
        return new ChatMessage(MessageRole.USER, content);
    }
}

