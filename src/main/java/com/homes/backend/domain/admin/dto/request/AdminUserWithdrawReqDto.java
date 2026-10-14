package com.homes.backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자용 유저 강제 탈퇴 요청 DTO")
public record AdminUserWithdrawReqDto(
        @Schema(description = "강제 탈퇴 사유", example = "허위 매물 반복 등록으로 인한 강제 탈퇴")
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자를 초과할 수 없습니다.")
        String reason
) {}
