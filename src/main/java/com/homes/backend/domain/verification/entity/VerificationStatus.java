package com.homes.backend.domain.verification.entity;

public enum VerificationStatus {
    PENDING,        // OCR 처리 대기 중
    APPROVED,       // 인증 승인 완료
    REJECTED,       // 인증 반려 (100m 초과 등)
    MANUAL_REVIEW   // 수동 검수 요망 (공동명의 등 예외 케이스)
}
