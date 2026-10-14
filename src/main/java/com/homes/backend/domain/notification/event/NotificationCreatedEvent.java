package com.homes.backend.domain.notification.event;

import com.homes.backend.domain.notification.dto.response.NotificationResDto;

public record NotificationCreatedEvent(Long userId, NotificationResDto notification) {}
