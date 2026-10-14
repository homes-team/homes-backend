package com.homes.backend.domain.realtor.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;

public record AgentProfileResDto(
        Long agentId,
        Long userId,
        String email,
        String officeName,
        String officeAddress,
        String businessNum,
        boolean isVerified
) {
    public static AgentProfileResDto from(Agent agent) {
        return new AgentProfileResDto(
                agent.getId(),
                agent.getUser().getId(),
                agent.getUser().getEmail(),
                agent.getOfficeName(),
                agent.getOfficeAddress(),
                agent.getBusinessNum(),
                agent.isVerified()
        );
    }
}
