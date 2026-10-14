package com.homes.backend.domain.admin.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;

import java.time.LocalDateTime;

public record AdminRealtorSummaryResDto(
        Long agentId,
        String name,
        String email,
        String officeName,
        String businessNum,
        boolean isVerified,
        LocalDateTime createdAt
) {
    public static AdminRealtorSummaryResDto from(Agent agent) {
        return new AdminRealtorSummaryResDto(
                agent.getId(),
                agent.getUser().getName(),
                agent.getUser().getEmail(),
                agent.getOfficeName(),
                agent.getBusinessNum(),
                agent.isVerified(),
                agent.getCreatedAt()
        );
    }
}
