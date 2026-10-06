package com.homes.backend.domain.notification.entity;

public enum NotificationType {
    CHAT,        // referenceId = chatId
    MATCHING,    // referenceId = propertyId
    SAFE_TRADE,  // referenceId = propertyId (현장 인증 기능 완성 전까지는 미사용)
    BID          // referenceId = propertyId (중개사가 새 입찰 제안서를 보냈을 때 집주인에게 전송)
}
