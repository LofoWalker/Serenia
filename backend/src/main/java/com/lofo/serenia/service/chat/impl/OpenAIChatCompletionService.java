package com.lofo.serenia.service.chat.impl;

import com.lofo.serenia.config.OpenAIConfig;
import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.service.chat.ChatCompletionService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.*;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OpenAIChatCompletionService implements ChatCompletionService {

    private final OpenAIClient client;
    private final OpenAIConfig config;

    public OpenAIChatCompletionService(OpenAIConfig config) {
        this.config = config;
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

    private static void addMessagesToRequest(List<ChatMessage> conversationMessages, List<ChatCompletionMessageParam> messages) {
        if (conversationMessages == null)
            return;

        for (ChatMessage content : conversationMessages) {
            addChatMessageToRequest(content, messages);
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

    private static void addChatMessageToRequest(ChatMessage content, List<ChatCompletionMessageParam> messages) {
        switch (content.role()) {
            case ASSISTANT -> messages.add(toAssistantMessageParam(content));
            case USER -> messages.add(toUserMessageParam(content));
        }
    }

    private static @NotNull ChatCompletionMessageParam toUserMessageParam(ChatMessage content) {
        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(content.content())
                        .build()
        );
    }

    private static @NotNull ChatCompletionMessageParam toAssistantMessageParam(ChatMessage content) {
        return ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                        .content(content.content())
                        .build()
        );
    }
}