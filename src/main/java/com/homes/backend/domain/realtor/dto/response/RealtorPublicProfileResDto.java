package com.homes.backend.domain.realtor.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;

public record RealtorPublicProfileResDto(
        Long agentId,
        String officeName,
        String officeAddress,
        String businessNum,
        boolean isVerified,
        Double successRate,
        Double averageReviewScore,
        long reviewCount
) {
    public static RealtorPublicProfileResDto of(
            Agent agent,
            Double successRate,
            Double averageReviewScore,
            long reviewCount
    ) {
        return new RealtorPublicProfileResDto(
                agent.getId(),
                agent.getOfficeName(),
                agent.getOfficeAddress(),
                agent.getBusinessNum(),
                agent.isVerified(),
                successRate,
                averageReviewScore,
                reviewCount
        );
    }
}
