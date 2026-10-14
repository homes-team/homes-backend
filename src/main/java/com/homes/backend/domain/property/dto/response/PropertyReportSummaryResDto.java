package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.property.entity.Property;

/**
 * 매물의 누적 신고 상태를 담습니다.
 *
 * @param propertyId 매물 ID
 * @param reportCount 누적 신고 횟수
 * @param isSuspicious 의심 매물 여부
 */
public record PropertyReportSummaryResDto(
        Long propertyId,
        Integer reportCount,
        boolean isSuspicious
) {
    /**
     * 매물 엔티티를 신고 요약 응답으로 변환합니다.
     *
     * @param property 변환할 매물
     * @return 매물 신고 요약
     */
    public static PropertyReportSummaryResDto from(Property property) {
        return new PropertyReportSummaryResDto(
                property.getId(),
                property.getReportCount(),
                property.isSuspicious()
        );
    }
}
