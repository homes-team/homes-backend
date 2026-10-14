package com.homes.backend.domain.admin.service;

import com.homes.backend.domain.admin.dto.request.AdminOwnerVerificationUpdateReqDto;
import com.homes.backend.domain.admin.dto.response.AdminOwnerVerificationListResDto;
import com.homes.backend.domain.verification.entity.OwnerVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.domain.verification.repository.OwnerVerificationRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminVerificationService {
    private final OwnerVerificationRepository ownerVerificationRepository;

    /**
     * 수동 검수(MANUAL_REVIEW) 대기 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AdminOwnerVerificationListResDto> getManualReviewList() {
        return ownerVerificationRepository.findAllByStatusOrderByRequestedAtDesc(VerificationStatus.MANUAL_REVIEW)
                .stream()
                .map(AdminOwnerVerificationListResDto::from)
                .toList();
    }

    /**
     * 수동 검수 결과 처리 (승인 또는 반려)
     */
    @Transactional
    public void processManualReview(Long verificationId, AdminOwnerVerificationUpdateReqDto reqDto) {
        //락(Lock)
        OwnerVerification verification = ownerVerificationRepository.findByIdWithLock(verificationId)
                .orElseThrow(() -> new CustomException(VerificationErrorCode.VERIFICATION_NOT_FOUND));

        // 이미 처리된 건인지 방어 로직
        if (verification.getStatus() != VerificationStatus.MANUAL_REVIEW) {
            throw new CustomException(VerificationErrorCode.NOT_MANUAL_REVIEW_STATUS);
        }

        // true면 APPROVED, false면 REJECTED
        VerificationStatus newStatus = reqDto.isApproved() ? VerificationStatus.APPROVED : VerificationStatus.REJECTED;

        verification.processAdminReview(newStatus);
    }
}
