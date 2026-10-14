package com.homes.backend.domain.verification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OwnerVerificationReqDto (
        @NotBlank(message = "등기부등본 이미지 URL은 필수입니다.")
        String documentUrl
){
}
