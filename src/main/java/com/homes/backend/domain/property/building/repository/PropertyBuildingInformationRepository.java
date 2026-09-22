package com.homes.backend.domain.property.building.repository;

import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PropertyBuildingInformationRepository extends JpaRepository<PropertyBuildingInformation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM PropertyBuildingInformation i WHERE i.propertyId = :propertyId")
    Optional<PropertyBuildingInformation> findByIdWithLock(@Param("propertyId") Long propertyId);

    @Query("SELECT i.propertyId FROM PropertyBuildingInformation i " +
            "WHERE i.status = com.homes.backend.domain.property.building.entity.BuildingInformationStatus.PENDING " +
            "OR (i.status = com.homes.backend.domain.property.building.entity.BuildingInformationStatus.PROCESSING " +
            "AND (i.lastAttemptAt IS NULL OR i.lastAttemptAt < :expiredBefore)) " +
            "ORDER BY i.lastAttemptAt ASC NULLS FIRST")
    List<Long> findRecoverablePropertyIds(@Param("expiredBefore") LocalDateTime expiredBefore, Pageable pageable);
}
