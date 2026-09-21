package com.homes.backend.domain.verification.dto.response;

import com.homes.backend.domain.verification.entity.VerificationStatus;

public record VerificationStatusRespDto(
        boolean isOwnerVerified,            // 집주인 인증 여부 (지금은 항상 false)
        VerificationStatus ownerStatus,     // 집주인 인증 상세 상태
        boolean isRealtorVerified,          // 중개사 현장 인증 여부 (APPROVED 일 때만 true)
        VerificationStatus realtorStatus    // 중개사 인증 상세 상태 (대기, 승인, 반려 등)
) {
}
