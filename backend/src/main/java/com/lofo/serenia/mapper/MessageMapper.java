package com.lofo.serenia.mapper;

import com.lofo.serenia.domain.conversation.ChatMessage;
import com.lofo.serenia.domain.conversation.Message;
import io.quarkus.arc.Unremovable;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {

    @Unremovable
    default ChatMessage toChatMessage(Message message, String decryptedContent) {
        if (message == null) {
            return null;
        }
        return new ChatMessage(message.getRole(), decryptedContent);
    }
}

