package com.homes.backend.domain.property.registry.service;

import com.homes.backend.domain.property.registry.entity.RegistryRiskLevel;

public record RegistryRiskScanResult(
        Integer mortgageCount,
        Integer seizureCount,
        RegistryRiskLevel riskLevel,
        String summary
) {
}
