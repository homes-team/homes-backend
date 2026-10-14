package com.homes.backend.domain.verification.repository;

import com.homes.backend.domain.verification.entity.RealtorVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RealtorVerificationRepository extends JpaRepository<RealtorVerification, Long> {
    // 특정 매물의 가장 최근 현장 인증 기록 1개를 가져오는 메서드
    Optional<RealtorVerification> findTopByPropertyIdOrderByRequestedAtDesc(Long propertyId);
}
