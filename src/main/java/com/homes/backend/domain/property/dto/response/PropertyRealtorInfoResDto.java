package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;

import java.util.List;

/**
 * 매물 담당 중개사 정보를 별도 API로 분리해 반환한다.
 * 아직 매칭된 중개사가 없어도(agent=null) 매물 정보 자체 조회는 막히지 않아야 하므로, 매물 상세 조회 API와 분리되어 있다.
 */
public record PropertyRealtorInfoResDto(
        AgentSummaryResDto agent,
        List<PropertyListRespDto> otherProperties
) {
    public record AgentSummaryResDto(
            Long agentId,
            String officeName,
            String officeAddress,
            boolean isVerified
    ) {
        public static AgentSummaryResDto from(Agent agent) {
            return new AgentSummaryResDto(
                    agent.getId(),
                    agent.getOfficeName(),
                    agent.getOfficeAddress(),
                    agent.isVerified()
            );
        }
    }

    public static PropertyRealtorInfoResDto of(Agent agent, List<PropertyListRespDto> otherProperties) {
        return new PropertyRealtorInfoResDto(AgentSummaryResDto.from(agent), otherProperties);
    }

    public static PropertyRealtorInfoResDto empty() {
        return new PropertyRealtorInfoResDto(null, List.of());
    }
}
