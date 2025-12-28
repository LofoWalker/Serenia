package com.lofo.serenia.mapper;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("ChatMessageMapper unit tests")
class ChatMessageMapperTest {

    private final ChatMessageMapper chatMessageMapper = new ChatMessageMapper() {
    };

    @Test
    @DisplayName("should_return_null_when_chat_message_is_null")
    void toChatCompletionMessageParamShouldReturnNullWhenChatMessageIsNull() {
        ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(null);

        assertNull(result);
    }

    @Test
    @DisplayName("should_convert_user_chat_message_to_completion_message_param")
    void toChatCompletionMessageParamShouldConvertUserChatMessage() {
        ChatMessage chatMessage = new ChatMessage(MessageRole.USER, "Hello");

        ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(chatMessage);

        assertNotNull(result);
    }

    @Test
    @DisplayName("should_convert_assistant_chat_message_to_completion_message_param")
    void toChatCompletionMessageParamShouldConvertAssistantChatMessage() {
        ChatMessage chatMessage = new ChatMessage(MessageRole.ASSISTANT, "Response");

        ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(chatMessage);

        assertNotNull(result);
    }

    @Test
    @DisplayName("should_convert_system_chat_message_to_completion_message_param")
    void toChatCompletionMessageParamShouldConvertSystemChatMessage() {
        ChatMessage chatMessage = new ChatMessage(MessageRole.SYSTEM, "System prompt");

        ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(chatMessage);

        assertNotNull(result);
    }

    @Test
    @DisplayName("should_handle_system_message_with_empty_content")
    void toChatCompletionMessageParamShouldHandleSystemMessageWithEmptyContent() {
        ChatMessage chatMessage = new ChatMessage(MessageRole.SYSTEM, "");

        ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(chatMessage);

        assertNotNull(result);
    }

    @Test
    @DisplayName("should_handle_system_message_with_long_content")
    void toChatCompletionMessageParamShouldHandleSystemMessageWithLongContent() {
        String longContent = "You are a helpful assistant. " + "Additional instructions ".repeat(100);
        ChatMessage chatMessage = new ChatMessage(MessageRole.SYSTEM, longContent);

        ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(chatMessage);

        assertNotNull(result);
    }
}

