package com.homes.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "관리자용 실명 인증 강제 동기화 요청 DTO")
public record AdminIdentityVerificationSyncReqDto(
        @Schema(description = "동기화 대상 유저 ID", example = "1")
        @NotNull(message = "userId는 필수입니다.")
        Long userId,

        @Schema(description = "유저가 문의 시 제공한 포트원 본인인증 고유 ID", example = "identity-verification-xxxx")
        @NotBlank(message = "identityVerificationId는 필수입니다.")
        String identityVerificationId
) {}
