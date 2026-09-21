package com.homes.backend.domain.verification.repository;

import com.homes.backend.domain.verification.entity.OwnerVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, Long> {
    Optional<OwnerVerification> findTopByPropertyIdOrderByRequestedAtDesc(Long propertyId);
}
