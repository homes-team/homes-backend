package com.homes.backend.domain.chat.dto.response;

import com.homes.backend.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResDto(
        Long chatId,
        Long propertyId,
        Long userId,
        Long agentUserId,
        boolean isUserLeft,
        boolean isAgentLeft,
        LocalDateTime createdAt
) {
    public static ChatRoomResDto from(ChatRoom room) {
        return new ChatRoomResDto(
                room.getId(),
                room.getProperty().getId(),
                room.getUser().getId(),
                room.getAgentUser().getId(),
                room.isUserLeft(),
                room.isAgentLeft(),
                room.getCreatedAt()
        );
    }
}
