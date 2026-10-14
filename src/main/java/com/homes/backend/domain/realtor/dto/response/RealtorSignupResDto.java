package com.homes.backend.domain.realtor.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.user.entity.User;

public record RealtorSignupResDto(
        Long userId,
        Long agentId,
        String email,
        String officeName,
        boolean isVerified
) {
    public static RealtorSignupResDto from(User user, Agent agent) {
        return new RealtorSignupResDto(
                user.getId(),
                agent.getId(),
                user.getEmail(),
                agent.getOfficeName(),
                agent.isVerified()
        );
    }
}
