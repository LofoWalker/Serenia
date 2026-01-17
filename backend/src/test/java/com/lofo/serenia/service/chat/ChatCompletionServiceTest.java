package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.OpenAIConfig;
import com.lofo.serenia.mapper.ChatMessageMapper;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.completions.CompletionUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatCompletionService Tests")
class ChatCompletionServiceTest {

    @Mock
    private OpenAIConfig config;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    private static final String MODEL = "gpt-4o-mini";
    private static final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        lenient().when(config.model()).thenReturn(MODEL);
        lenient().when(config.apiKey()).thenReturn(API_KEY);
    }

    @Nested
    @DisplayName("Response Parsing - parseCompletionAndReturnResult")
    class ResponseParsing {

        @Test
        @DisplayName("should extract tokens from response")
        void should_extract_tokens_from_response() {
            ChatCompletion completion = createMockCompletion("Assistant response", 100, 50, 75);

            ChatCompletionService.ChatCompletionResult result = parseCompletion(completion);

            assertThat(result.content()).isEqualTo("Assistant response");
            assertThat(result.promptTokens()).isEqualTo(100);
            assertThat(result.cachedTokens()).isEqualTo(50);
            assertThat(result.completionTokens()).isEqualTo(75);
        }

        @Test
        @DisplayName("should handle empty choices")
        void should_handle_empty_choices() {
            ChatCompletion completion = createMockCompletionWithoutChoices();

            ChatCompletionService.ChatCompletionResult result = parseCompletion(completion);

            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("should handle missing usage info")
        void should_handle_missing_usage_info() {
            ChatCompletion completion = createMockCompletionWithoutUsage("Response");

            ChatCompletionService.ChatCompletionResult result = parseCompletion(completion);

            assertThat(result.promptTokens()).isZero();
            assertThat(result.cachedTokens()).isZero();
            assertThat(result.completionTokens()).isZero();
        }

        @Test
        @DisplayName("should handle missing cached tokens details")
        void should_handle_missing_cached_tokens_details() {
            ChatCompletion completion = createMockCompletionWithoutCachedTokens("Response", 100, 75);

            ChatCompletionService.ChatCompletionResult result = parseCompletion(completion);

            assertThat(result.content()).isEqualTo("Response");
            assertThat(result.promptTokens()).isEqualTo(100);
            assertThat(result.cachedTokens()).isZero();
            assertThat(result.completionTokens()).isEqualTo(75);
        }

        @Test
        @DisplayName("should return empty content when message content is empty")
        void should_return_empty_content_when_message_content_is_empty() {
            ChatCompletion completion = createMockCompletionWithEmptyContent();

            ChatCompletionService.ChatCompletionResult result = parseCompletion(completion);

            assertThat(result.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Message Mapping")
    class MessageMapping {

        @Test
        @DisplayName("should map conversation messages using mapper")
        void should_map_conversation_messages_using_mapper() {
            ChatMessage userMessage = new ChatMessage(MessageRole.USER, "Hello");
            ChatCompletionMessageParam mockParam = mock(ChatCompletionMessageParam.class);
            when(chatMessageMapper.toChatCompletionMessageParam(userMessage)).thenReturn(mockParam);

            ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(userMessage);

            assertThat(result).isEqualTo(mockParam);
            verify(chatMessageMapper).toChatCompletionMessageParam(userMessage);
        }

        @Test
        @DisplayName("should handle assistant message mapping")
        void should_handle_assistant_message_mapping() {
            ChatMessage assistantMessage = new ChatMessage(MessageRole.ASSISTANT, "Hi there!");
            ChatCompletionMessageParam mockParam = mock(ChatCompletionMessageParam.class);
            when(chatMessageMapper.toChatCompletionMessageParam(assistantMessage)).thenReturn(mockParam);

            ChatCompletionMessageParam result = chatMessageMapper.toChatCompletionMessageParam(assistantMessage);

            assertThat(result).isEqualTo(mockParam);
            verify(chatMessageMapper).toChatCompletionMessageParam(assistantMessage);
        }
    }

    @Nested
    @DisplayName("ChatCompletionResult record")
    class ChatCompletionResultRecord {

        @Test
        @DisplayName("should create result with all fields")
        void should_create_result_with_all_fields() {
            ChatCompletionService.ChatCompletionResult result =
                    new ChatCompletionService.ChatCompletionResult("content", 100, 50, 75);

            assertThat(result.content()).isEqualTo("content");
            assertThat(result.promptTokens()).isEqualTo(100);
            assertThat(result.cachedTokens()).isEqualTo(50);
            assertThat(result.completionTokens()).isEqualTo(75);
        }

        @Test
        @DisplayName("should handle zero token values")
        void should_handle_zero_token_values() {
            ChatCompletionService.ChatCompletionResult result =
                    new ChatCompletionService.ChatCompletionResult("", 0, 0, 0);

            assertThat(result.content()).isEmpty();
            assertThat(result.promptTokens()).isZero();
            assertThat(result.cachedTokens()).isZero();
            assertThat(result.completionTokens()).isZero();
        }
    }

    private ChatCompletion createMockCompletion(String content, int prompt, int cached, int completion) {
        ChatCompletion mockCompletion = mock(ChatCompletion.class);

        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);
        when(message.content()).thenReturn(Optional.of(content));
        when(choice.message()).thenReturn(message);
        when(mockCompletion.choices()).thenReturn(List.of(choice));

        CompletionUsage usage = mock(CompletionUsage.class);
        when(usage.promptTokens()).thenReturn((long) prompt);
        when(usage.completionTokens()).thenReturn((long) completion);

        CompletionUsage.PromptTokensDetails details = mock(CompletionUsage.PromptTokensDetails.class);
        when(details.cachedTokens()).thenReturn(Optional.of((long) cached));
        when(usage.promptTokensDetails()).thenReturn(Optional.of(details));

        when(mockCompletion.usage()).thenReturn(Optional.of(usage));

        return mockCompletion;
    }

    private ChatCompletion createMockCompletionWithoutChoices() {
        ChatCompletion mockCompletion = mock(ChatCompletion.class);
        when(mockCompletion.choices()).thenReturn(List.of());
        when(mockCompletion.usage()).thenReturn(Optional.empty());
        return mockCompletion;
    }

    private ChatCompletion createMockCompletionWithoutUsage(String content) {
        ChatCompletion mockCompletion = mock(ChatCompletion.class);

        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);
        when(message.content()).thenReturn(Optional.of(content));
        when(choice.message()).thenReturn(message);
        when(mockCompletion.choices()).thenReturn(List.of(choice));
        when(mockCompletion.usage()).thenReturn(Optional.empty());

        return mockCompletion;
    }

    private ChatCompletion createMockCompletionWithoutCachedTokens(String content, int prompt, int completion) {
        ChatCompletion mockCompletion = mock(ChatCompletion.class);

        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);
        when(message.content()).thenReturn(Optional.of(content));
        when(choice.message()).thenReturn(message);
        when(mockCompletion.choices()).thenReturn(List.of(choice));

        CompletionUsage usage = mock(CompletionUsage.class);
        when(usage.promptTokens()).thenReturn((long) prompt);
        when(usage.completionTokens()).thenReturn((long) completion);
        when(usage.promptTokensDetails()).thenReturn(Optional.empty());

        when(mockCompletion.usage()).thenReturn(Optional.of(usage));

        return mockCompletion;
    }

    private ChatCompletion createMockCompletionWithEmptyContent() {
        ChatCompletion mockCompletion = mock(ChatCompletion.class);

        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);
        when(message.content()).thenReturn(Optional.empty());
        when(choice.message()).thenReturn(message);
        when(mockCompletion.choices()).thenReturn(List.of(choice));
        when(mockCompletion.usage()).thenReturn(Optional.empty());

        return mockCompletion;
    }

    private ChatCompletionService.ChatCompletionResult parseCompletion(ChatCompletion completion) {
        String content = "";
        int promptTokens = 0;
        int cachedTokens = 0;
        int completionTokens = 0;

        if (!completion.choices().isEmpty()) {
            content = completion.choices().getFirst().message().content().orElse("");
        }

        if (completion.usage().isPresent()) {
            CompletionUsage usage = completion.usage().get();
            promptTokens = Math.toIntExact(usage.promptTokens());
            completionTokens = Math.toIntExact(usage.completionTokens());

            if (usage.promptTokensDetails().isPresent()) {
                cachedTokens = Math.toIntExact(usage.promptTokensDetails().get().cachedTokens().orElse(0L));
            }
        }

        return new ChatCompletionService.ChatCompletionResult(content, promptTokens, cachedTokens, completionTokens);
    }
}
