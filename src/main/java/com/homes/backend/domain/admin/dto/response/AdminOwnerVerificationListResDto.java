package com.homes.backend.domain.admin.dto.response;

import com.homes.backend.domain.verification.entity.OwnerVerification;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminOwnerVerificationListResDto(
        Long verificationId,
        Long propertyId,
        String documentUrl,
        String failReason,
        LocalDateTime requestedAt
) {
    public static AdminOwnerVerificationListResDto from(OwnerVerification verification) {
        return AdminOwnerVerificationListResDto.builder()
                .verificationId(verification.getId())
                .propertyId(verification.getProperty().getId())
                .documentUrl(verification.getDocumentUrl())
                .failReason(verification.getFailReason())
                .requestedAt(verification.getRequestedAt())
                .build();
    }
}
