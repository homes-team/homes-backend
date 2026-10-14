package com.homes.backend.domain.notification.controller;

import com.homes.backend.domain.notification.dto.response.NotificationResDto;
import com.homes.backend.domain.notification.service.NotificationService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import com.homes.backend.global.security.WebSocketTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/users/me/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final NotificationService notificationService;
    private final WebSocketTicketService webSocketTicketService;

    @Override
    @GetMapping
    public ApiResponse<List<NotificationResDto>> getMyNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<NotificationResDto> response = notificationService.getMyNotifications(userPrincipal.getId());
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PatchMapping
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }

    @Override
    @PatchMapping("/{notificationId}")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        notificationService.markAsRead(notificationId, userPrincipal.getId());
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "실시간 알림 연결 (SSE)", description = "POST /ws/tickets로 발급받은 1회용 티켓으로 실시간 알림 스트림에 연결합니다. " +
            "브라우저 EventSource는 커스텀 헤더를 못 붙여서 Authorization 대신 쿼리 파라미터 티켓으로 인증합니다.")
    @Tag(name = "알림(Notification) API")
    @GetMapping("/stream")
    public SseEmitter connectStream(@RequestParam String ticket) {
        Long userId = webSocketTicketService.consumeTicket(ticket);
        if (userId == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }
        return notificationService.connect(userId);
    }
}
