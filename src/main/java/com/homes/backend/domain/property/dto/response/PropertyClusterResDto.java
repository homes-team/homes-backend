package com.homes.backend.domain.property.dto.response;

import lombok.Builder;

@Builder
public record PropertyClusterResDto(
        Double lat,
        Double lon,
        Long count
) {
    public static PropertyClusterResDto from(ClusterProjection projection) {
        return PropertyClusterResDto.builder()
                .lat(projection.getLat())
                .lon(projection.getLon())
                .count(projection.getCount())
                .build();
    }

    public interface ClusterProjection {
        Double getLat();
        Double getLon();
        Long getCount();
    }
}
