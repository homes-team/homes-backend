package com.homes.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserUpdatePasswordReqDto(
        @NotBlank(message = "현재 비밀번호는 필수 입력 항목입니다.")
        String currentPassword,

        @NotBlank(message = "새로운 비밀번호는 필수 입력 항목입니다.")
        String newPassword
) {}