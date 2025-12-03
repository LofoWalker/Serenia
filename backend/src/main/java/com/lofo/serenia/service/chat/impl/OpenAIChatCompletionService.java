package com.lofo.serenia.service.chat.impl;

import com.lofo.serenia.config.OpenAIConfig;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.mapper.ChatMessageMapper;
import com.lofo.serenia.service.chat.ChatCompletionService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OpenAIChatCompletionService implements ChatCompletionService {

    private final OpenAIClient client;
    private final OpenAIConfig config;
    private final ChatMessageMapper chatMessageMapper;

    @Inject
    public OpenAIChatCompletionService(OpenAIConfig config, ChatMessageMapper chatMessageMapper) {
        this.config = config;
        this.chatMessageMapper = chatMessageMapper;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(this.config.apiKey())
                .build();
    }

    @Override
    public String generateReply(String systemPrompt, List<ChatMessage> conversationMessages) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        addSystemInstructionsToRequest(systemPrompt, messages);
        addMessagesToRequest(conversationMessages, messages);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(this.config.model())
                .messages(messages)
                .build();

        ChatCompletion completion = sendRequestAndGetCompletion(params);

        return parseCompletionAndReturnResponse(completion);
    }

    private static @NotNull String parseCompletionAndReturnResponse(ChatCompletion completion) {
        if (completion.choices().isEmpty()) {
            return "";
        }

        return completion.choices().getFirst().message().content().orElse("");
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