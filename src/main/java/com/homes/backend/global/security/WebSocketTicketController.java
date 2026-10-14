package com.homes.backend.global.security;

import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "웹소켓 인증", description = "웹소켓 핸드셰이크용 1회용 티켓 발급")
@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
public class WebSocketTicketController {

    private final WebSocketTicketService webSocketTicketService;

    @Operation(summary = "웹소켓 접속 티켓 발급", description = "일반 REST 요청(Authorization 헤더)으로 인증한 뒤, " +
            "웹소켓 접속 URL에 붙일 30초짜리 1회용 티켓을 발급받습니다. 발급 후 곧바로 접속에 사용해야 합니다.")
    @PostMapping("/tickets")
    public ApiResponse<WebSocketTicketResDto> issueTicket(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        String ticket = webSocketTicketService.issueTicket(userPrincipal.getId());
        return ApiResponse.onSuccess(new WebSocketTicketResDto(ticket));
    }
}
