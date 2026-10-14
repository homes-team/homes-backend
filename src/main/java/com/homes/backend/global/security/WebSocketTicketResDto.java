package com.homes.backend.global.security;

import io.swagger.v3.oas.annotations.media.Schema;

public record WebSocketTicketResDto(
        @Schema(description = "웹소켓 접속용 1회용 티켓 (30초 내 미사용 시 만료)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String ticket
) {}
