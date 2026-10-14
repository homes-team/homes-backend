package com.homes.backend.domain.user.controller;

import com.homes.backend.domain.user.dto.request.UserReportCreateReqDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "유저 신고(User Report) API", description = "채팅 등에서 문제가 있었던 유저 신고 접수 API")
public interface UserReportControllerDocs {
    @Operation(summary = "유저 신고 접수", description = "문제가 있는 유저를 신고합니다. 실제로 채팅한 적 있는 상대만 신고할 수 있고, " +
            "같은 유저를 중복 신고할 수 없으며, 누적 신고 5건 시 의심 유저로 자동 전환됩니다.")
    ApiResponse<Void> reportUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "신고할 유저의 ID", required = true) @PathVariable("userId") Long userId,
            @RequestBody @Valid UserReportCreateReqDto reqDto
    );
}
