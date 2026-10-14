package com.homes.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "실명 인증 요청 DTO")
public record IdentityVerificationReqDto(
        @Schema(description = "프론트엔드에서 포트원 SDK로 본인인증을 완료한 뒤 발급받은 고유 ID", example = "identity-verification-xxxx")
        @NotBlank(message = "identityVerificationId는 필수입니다.")
        String identityVerificationId
) {}
