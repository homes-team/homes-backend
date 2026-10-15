package com.homes.backend.domain.property.insight.dto;

import java.util.List;

public record NearbyFacilitiesRespDto(
        Long propertyId,
        Center center,
        int radiusMeters,
        List<Facility> facilities
) {
    public record Center(double latitude, double longitude) {}

    public record Facility(
            String id,
            FacilityType type,
            String name,
            double latitude,
            double longitude,
            long distanceMeters,
            String address,
            FacilitySource source
    ) {}

    public enum FacilityType {
        SUBWAY,
        BUS_STOP,
        SCHOOL,
        HOSPITAL,
        CULTURE,
        PARK,
        MART
    }

    public enum FacilitySource {
        NATIONWIDE_TRANSPORT_POI,
        KAKAO_LOCAL
    }
}
