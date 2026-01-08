package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.OpenAIConfig;
import com.lofo.serenia.mapper.ChatMessageMapper;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ReasoningEffort;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.completions.CompletionUsage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates assistant replies using OpenAI API.
 */
@Slf4j
@ApplicationScoped
public class ChatCompletionService {

    private final OpenAIClient client;
    private final OpenAIConfig config;
    private final ChatMessageMapper chatMessageMapper;

    public record ChatCompletionResult(String content, int totalTokensUsed) {}

    @Inject
    public ChatCompletionService(OpenAIConfig config, ChatMessageMapper chatMessageMapper) {
        this.config = config;
        this.chatMessageMapper = chatMessageMapper;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(this.config.apiKey())
                .build();
    }

    /**
     * Generates an assistant reply using the system prompt plus decrypted conversation history.
     * Returns both the response content and the actual tokens consumed by OpenAI.
     */
    public ChatCompletionResult generateReply(String systemPrompt, List<ChatMessage> conversationMessages) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        addSystemInstructionsToRequest(systemPrompt, messages);
        addMessagesToRequest(conversationMessages, messages);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(this.config.model())
                .messages(messages)
                .build();

        ChatCompletion completion = sendRequestAndGetCompletion(params);

        return parseCompletionAndReturnResult(completion);
    }

    private static @NotNull ChatCompletionResult parseCompletionAndReturnResult(ChatCompletion completion) {
        String content = "";
        int totalTokensUsed = 0;

        if (!completion.choices().isEmpty()) {
            content = completion.choices().getFirst().message().content().orElse("");
        }

        if (completion.usage().isPresent()) {
            totalTokensUsed = Math.toIntExact(completion.usage().get().totalTokens());
            log.debug("{} tokens used by OpenAI API", totalTokensUsed);
        } else {
            log.warn("OpenAI API did not return usage information");
        }

        return new ChatCompletionResult(content, totalTokensUsed);
    }

    private @NotNull ChatCompletion sendRequestAndGetCompletion(ChatCompletionCreateParams params) {
        return client.chat().completions().create(params);
    }

    private void addMessagesToRequest(List<ChatMessage> conversationMessages, List<ChatCompletionMessageParam> messages) {
        if (conversationMessages == null)
            return;

        for (ChatMessage content : conversationMessages) {
            messages.add(chatMessageMapper.toChatCompletionMessageParam(content));
        }
    }

    private static void addSystemInstructionsToRequest(String systemPrompt, List<ChatCompletionMessageParam> messages) {
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                            .content(systemPrompt)
                            .build()
            ));
        }
    }
}

