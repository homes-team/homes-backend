package com.homes.backend.domain.chat.dto.response;

import com.homes.backend.domain.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageListResDto(
        Long messageId,
        String content,
        boolean isRead,
        Long senderId,
        LocalDateTime createdAt
) {
    public static ChatMessageListResDto from(ChatMessage message) {
        return new ChatMessageListResDto(
                message.getId(),
                message.getContent(),
                message.isRead(),
                message.getSender().getId(),
                message.getCreatedAt()
        );
    }
}
