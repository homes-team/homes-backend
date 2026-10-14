package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.realtor.entity.Agent;

import java.util.List;

/**
 * 매물 담당 중개사 정보를 별도 API로 분리해 반환한다.
 * 아직 매칭된 중개사가 없어도(agent=null) 매물 정보 자체 조회는 막히지 않아야 하므로, 매물 상세 조회 API와 분리되어 있다.
 *
 * @param agent 담당 중개사 요약, 매칭된 중개사가 없으면 {@code null}
 * @param otherProperties 담당 중개사의 다른 매물 목록
 */
public record PropertyRealtorInfoResDto(
        AgentSummaryResDto agent,
        List<PropertyListRespDto> otherProperties
) {
    /**
     * 매물 담당 중개사의 공개 정보를 담습니다.
     *
     * @param agentId 중개사 ID
     * @param officeName 중개사무소 이름
     * @param officeAddress 중개사무소 주소
     * @param isVerified 중개사 인증 여부
     */
    public record AgentSummaryResDto(
            Long agentId,
            String officeName,
            String officeAddress,
            boolean isVerified
    ) {
        /**
         * 중개사 엔티티를 공개 정보 요약으로 변환합니다.
         *
         * @param agent 변환할 중개사
         * @return 중개사 공개 정보 요약
         */
        public static AgentSummaryResDto from(Agent agent) {
            return new AgentSummaryResDto(
                    agent.getId(),
                    agent.getOfficeName(),
                    agent.getOfficeAddress(),
                    agent.isVerified()
            );
        }
    }

    /**
     * 담당 중개사와 다른 매물 목록을 응답으로 변환합니다.
     *
     * @param agent 담당 중개사
     * @param otherProperties 담당 중개사의 다른 매물 목록
     * @return 담당 중개사 정보 응답
     */
    public static PropertyRealtorInfoResDto of(Agent agent, List<PropertyListRespDto> otherProperties) {
        return new PropertyRealtorInfoResDto(AgentSummaryResDto.from(agent), otherProperties);
    }

    /**
     * 아직 담당 중개사가 없는 매물의 빈 응답을 생성합니다.
     *
     * @return 담당 중개사가 없고 다른 매물 목록이 비어 있는 응답
     */
    public static PropertyRealtorInfoResDto empty() {
        return new PropertyRealtorInfoResDto(null, List.of());
    }
}
