package com.homes.backend.domain.property.registry.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistryRiskLevel {
    SAFE("안전"),
    CAUTION("주의"),
    DANGER("위험");

    private final String description;
}
