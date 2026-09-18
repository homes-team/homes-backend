package com.homes.backend.domain.property.registry.dto.response;

import com.homes.backend.domain.property.registry.entity.PropertyRegistryRisk;
import com.homes.backend.domain.property.registry.entity.RegistryRiskLevel;

import java.time.LocalDateTime;

public record PropertyRegistryRiskResDto(
        Long propertyId,
        RegistryRiskLevel riskLevel,
        Integer mortgageCount,
        Integer seizureCount,
        String summary,
        LocalDateTime scannedAt
) {
    public static PropertyRegistryRiskResDto from(PropertyRegistryRisk registryRisk) {
        return new PropertyRegistryRiskResDto(
                registryRisk.getProperty().getId(),
                registryRisk.getRiskLevel(),
                registryRisk.getMortgageCount(),
                registryRisk.getSeizureCount(),
                registryRisk.getSummary(),
                registryRisk.getUpdatedAt()
        );
    }
}
