package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.property.entity.Property;

public record PropertyReportSummaryResDto(
        Long propertyId,
        Integer reportCount,
        boolean isSuspicious
) {
    public static PropertyReportSummaryResDto from(Property property) {
        return new PropertyReportSummaryResDto(
                property.getId(),
                property.getReportCount(),
                property.isSuspicious()
        );
    }
}
