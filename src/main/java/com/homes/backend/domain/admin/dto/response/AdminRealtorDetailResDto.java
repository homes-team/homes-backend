package com.homes.backend.domain.admin.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;

import java.time.LocalDateTime;

public record AdminRealtorDetailResDto(
        Long agentId,
        Long userId,
        String name,
        String email,
        String phone,
        String officeName,
        String officeAddress,
        String businessNum,
        String profileImageUrl,
        String businessCertUrl,
        String agentCertUrl,
        boolean isVerified,
        LocalDateTime createdAt
) {
    public static AdminRealtorDetailResDto from(Agent agent) {
        return new AdminRealtorDetailResDto(
                agent.getId(),
                agent.getUser().getId(),
                agent.getUser().getName(),
                agent.getUser().getEmail(),
                agent.getUser().getPhone(),
                agent.getOfficeName(),
                agent.getOfficeAddress(),
                agent.getBusinessNum(),
                agent.getProfileImageUrl(),
                agent.getBusinessCertUrl(),
                agent.getAgentCertUrl(),
                agent.isVerified(),
                agent.getCreatedAt()
        );
    }
}
