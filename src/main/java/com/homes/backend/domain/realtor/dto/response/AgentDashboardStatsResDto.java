package com.homes.backend.domain.realtor.dto.response;

import com.homes.backend.domain.property.entity.PropertyType;

import java.util.List;

public record AgentDashboardStatsResDto(
        long thisMonthCompletedDealsCount,
        List<AverageFeeByPropertyType> averageFeesByPropertyType
) {
    public record AverageFeeByPropertyType(
            PropertyType propertyType,
            Double averageFee
    ) {}
}
