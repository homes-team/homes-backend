package com.homes.backend.domain.notification.controller;

import com.homes.backend.domain.notification.dto.response.NotificationResDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "알림(Notification) API", description = "알림 목록 조회/읽음 처리. 실시간 알림(SSE)은 /users/me/notifications/stream 참고")
public interface NotificationControllerDocs {

    @Operation(summary = "내 알림 목록 조회", description = "최신순으로 내 알림 목록을 조회합니다.")
    ApiResponse<List<NotificationResDto>> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "알림 전체 읽음 처리", description = "안 읽은 내 알림을 전부 읽음 처리합니다.")
    ApiResponse<Void> markAllAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "알림 개별 읽음 처리", description = "특정 알림 하나만 읽음 처리합니다. 내 알림이 아니면 실패합니다.")
    ApiResponse<Void> markAsRead(
            @PathVariable Long notificationId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );
}
