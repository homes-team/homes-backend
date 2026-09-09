package com.homes.backend.domain.property.insight.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IsochroneRespDto(
        Long propertyId,
        String mode,
        int travelTimeMinutes,
        Center center,
        Geometry geometry,
        Metadata metadata
) {
    public record Center(double latitude, double longitude) {}
    public record Geometry(String type, List<List<List<Double>>> coordinates) {}
    public record Metadata(
            Integer estimatedDistanceMeters,
            double totalAreaSquareMeters,
            String calculationMethod,
            String dataSource,
            boolean cached,
            LocalDateTime generatedAt
    ) {}
}
