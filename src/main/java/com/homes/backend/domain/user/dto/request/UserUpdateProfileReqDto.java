package com.homes.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "마이페이지 회원정보 수정 요청 DTO. 값을 보내지 않은 필드는 기존 값이 유지됩니다.")
public record UserUpdateProfileReqDto(
        @Schema(description = "닉네임", example = "행복한둥이")
        @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하로 입력해주세요.")
        String nickname,

        @Schema(description = "이용 목적", example = "투자")
        @Size(min = 1, max = 50, message = "이용 목적은 1자 이상 50자 이하로 입력해주세요.")
        String usagePurpose
) {}
