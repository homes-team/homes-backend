package com.homes.backend.domain.infrastructure.dto.response;

import com.homes.backend.domain.infrastructure.entity.Infrastructure;
import lombok.Builder;

@Builder
public record InfrastructureResDto(
        Long id,
        String name,
        String infraType,
        Double lat,
        Double lon
) {
    public static InfrastructureResDto from(Infrastructure infra) {
        return InfrastructureResDto.builder()
                .id(infra.getId())
                .name(infra.getName())
                .infraType(infra.getInfraType().name())
                .lat(infra.getLat())
                .lon(infra.getLon())
                .build();
    }
}
