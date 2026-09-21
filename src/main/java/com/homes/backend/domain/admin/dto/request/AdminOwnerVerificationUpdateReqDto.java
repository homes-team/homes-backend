package com.homes.backend.domain.admin.dto.request;

public record AdminOwnerVerificationUpdateReqDto(
        boolean isApproved // true면 승인(APPROVED), false면 반려(REJECTED)
) {
}
