package com.homes.backend.domain.user.dto.request;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record OAuthLoginReqDto(
        @Schema(description = "구글 인증 코드", example = "4/0AfgeXv...")
        @NotBlank(message = "구글 인증 코드는 필수입니다.")
        String authorizationCode
) {}