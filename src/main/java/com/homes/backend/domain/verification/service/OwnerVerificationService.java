package com.homes.backend.domain.verification.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.domain.verification.dto.request.OwnerVerificationReqDto;
import com.homes.backend.domain.verification.entity.OwnerVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.domain.verification.repository.OwnerVerificationRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OwnerVerificationService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final OwnerVerificationRepository ownerVerificationRepository;
    private final OwnerVerificationWorker ownerVerificationWorker;

    public void requestOwnerVerification(Long propertyId, Long userId, OwnerVerificationReqDto reqDto) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 실명(name)이 없거나 본인 인증을 안 한 유저면 튕겨냄
        if (!user.isIdentityVerified() || user.getName() == null || user.getName().isBlank()) {
            throw new CustomException(UserErrorCode.IDENTITY_VERIFICATION_NOT_COMPLETED);
        }

        // 최근 인증 내역 조회 및 중복 방지
        OwnerVerification latestVerification = ownerVerificationRepository
                .findTopByPropertyIdOrderByRequestedAtDesc(propertyId)
                .orElse(null);

        if (latestVerification != null) {
            if (latestVerification.getStatus() == VerificationStatus.APPROVED) {
                throw new CustomException(VerificationErrorCode.OWNER_ALREADY_VERIFIED);
            }
            if (latestVerification.getStatus() == VerificationStatus.PENDING) {
                throw new CustomException(VerificationErrorCode.OWNER_VERIFICATION_IN_PROGRESS);
            }
        }

        // 일단 PENDING 상태로 DB에 저장
        OwnerVerification verification = OwnerVerification.builder()
                .property(property)
                .user(user)
                .documentUrl(reqDto.documentUrl())
                .status(VerificationStatus.PENDING)
                .build();

        ownerVerificationRepository.save(verification);

        // 비동기 Worker에게 OCR 처리 지시 (메서드는 여기서 종료되고 프론트에 바로 응답 됨)
        // (주의: 트랜잭션 분리를 위해 생성된 verification의 ID와 유저 실명을 넘김)
        ownerVerificationWorker.processOcrVerification(verification.getId(), reqDto.documentUrl(), user.getName());
    }
}
