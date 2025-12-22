package com.lofo.serenia.mapper;

import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import io.quarkus.arc.Unremovable;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMessageMapper {

    @Unremovable
    @Named("toChatCompletionMessageParam")
    default ChatCompletionMessageParam toChatCompletionMessageParam(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return null;
        }
        return switch (chatMessage.role()) {
            case ASSISTANT -> toAssistantMessageParam(chatMessage);
            case USER -> toUserMessageParam(chatMessage);
            case SYSTEM -> toSystemMessageParam(chatMessage);
        };
    }

    @Unremovable
    private static @NotNull ChatCompletionMessageParam toUserMessageParam(ChatMessage content) {
        return ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(content.content())
                        .build()
        );
    }

    @Unremovable
    private static @NotNull ChatCompletionMessageParam toAssistantMessageParam(ChatMessage content) {
        return ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                        .content(content.content())
                        .build()
        );
    }

    @Unremovable
    private static @NotNull ChatCompletionMessageParam toSystemMessageParam(ChatMessage content) {
        return ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(content.content())
                        .build()
        );
    }
}

