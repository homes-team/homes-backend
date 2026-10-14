package com.homes.backend.domain.notification.dto.response;

import com.homes.backend.domain.notification.entity.Notification;
import com.homes.backend.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResDto(
        Long notificationId,
        String content,
        boolean isRead,
        NotificationType type,
        Long referenceId,
        LocalDateTime createdAt
) {
    public static NotificationResDto from(Notification notification) {
        return new NotificationResDto(
                notification.getId(),
                notification.getContent(),
                notification.isRead(),
                notification.getType(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}
