package com.homes.backend.domain.verification.repository;

import com.homes.backend.domain.verification.entity.OwnerVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, Long> {
    Optional<OwnerVerification> findTopByPropertyIdOrderByRequestedAtDesc(Long propertyId);

    // 관리자용: 특정 상태(예: MANUAL_REVIEW)의 인증 내역을 최신순으로 조회
    List<OwnerVerification> findAllByStatusOrderByRequestedAtDesc(VerificationStatus status);

    // 관리자 수동 검수 시 동시성 충돌을 막기 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM OwnerVerification v WHERE v.id = :id")
    Optional<OwnerVerification> findByIdWithLock(@Param("id") Long id);
}
