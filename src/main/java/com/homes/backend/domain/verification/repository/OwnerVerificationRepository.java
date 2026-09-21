package com.homes.backend.domain.verification.repository;

import com.homes.backend.domain.verification.entity.OwnerVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, Long> {
    Optional<OwnerVerification> findTopByPropertyIdOrderByRequestedAtDesc(Long propertyId);

    // 관리자용: 특정 상태(예: MANUAL_REVIEW)의 인증 내역을 최신순으로 조회
    List<OwnerVerification> findAllByStatusOrderByRequestedAtDesc(VerificationStatus status);
}
