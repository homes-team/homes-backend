package com.homes.backend.domain.admin.service;

import com.homes.backend.domain.admin.dto.response.AdminPropertyReportDetailResDto;
import com.homes.backend.domain.admin.dto.response.AdminReportedPropertyResDto;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyReportRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyReportRepository propertyReportRepository;

    /**
     * 신고 1건 이상 매물 목록. 신고 많은 순 정렬(의심 매물이 자연히 위쪽에 몰림), 이미 삭제된 매물은 제외
     */
    public List<AdminReportedPropertyResDto> getReportedProperties() {
        return propertyRepository.findByReportCountGreaterThanAndStatusNotOrderByReportCountDesc(0, PropertyStatus.DELETED).stream()
                .map(AdminReportedPropertyResDto::from)
                .toList();
    }

    /**
     * 의심 매물 전체 목록. 신고 누적/규칙 기반 자동탐지/등기부등본 스캔/관리자 수동 지정 등
     * 출처와 무관하게 isSuspicious=true인 매물을 전부 모아서 보여준다. 최근 판정 순 정렬.
     */
    public List<AdminReportedPropertyResDto> getSuspiciousProperties() {
        return propertyRepository.findSuspiciousProperties(PropertyStatus.DELETED).stream()
                .map(AdminReportedPropertyResDto::from)
                .toList();
    }

    /**
     * 특정 매물의 신고 상세 내역. 의심 매물 여부, 삭제 여부와 무관하게 조회 가능(감사 목적)
     */
    public List<AdminPropertyReportDetailResDto> getPropertyReportDetail(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND);
        }

        return propertyReportRepository.findAllByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
                .map(AdminPropertyReportDetailResDto::from)
                .toList();
    }

    /**
     * 허위 매물 강제 삭제. 일반 유저의 매물 삭제와 동일하게 소프트 삭제로 처리하되,
     * 소유권 검증 없이 관리자가 임의의 매물을 대상으로 할 수 있다
     */
    @Transactional
    public void deleteProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        property.markAsDeleted();
    }

    /**
     * 의심 매물 여부를 관리자가 직접 지정/해제한다. 신고 누적 자동 전환, 규칙 기반 자동탐지와는
     * 별개의 수동 경로로, 삭제까지는 아니지만 주의가 필요하다고 판단될 때 사용한다.
     */
    @Transactional
    public void updateSuspiciousStatus(Long propertyId, boolean isSuspicious) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        property.markSuspicious(isSuspicious);
    }
}
