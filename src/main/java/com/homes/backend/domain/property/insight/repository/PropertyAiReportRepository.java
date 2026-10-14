package com.homes.backend.domain.property.insight.repository;

import com.homes.backend.domain.property.insight.entity.PropertyAiReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyAiReportRepository extends JpaRepository<PropertyAiReport, Long> {
    Optional<PropertyAiReport> findByPropertyIdAndInputHash(Long propertyId, String inputHash);
}
