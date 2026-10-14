package com.homes.backend.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminOwnerVerificationUpdateReqDto(
        @NotNull(message = "승인 여부 값은 필수입니다.")
        boolean isApproved // true면 승인(APPROVED), false면 반려(REJECTED)
) {
}
