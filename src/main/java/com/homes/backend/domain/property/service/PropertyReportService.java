package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.dto.request.ReportCreateReqDto;
import com.homes.backend.domain.property.dto.response.PropertyReportSummaryResDto;
import com.homes.backend.domain.property.dto.response.ReportListRespDto;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyReport;
import com.homes.backend.domain.property.entity.ReportReason;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyReportRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyReportService {

    private final PropertyReportRepository reportRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    /**
     * 신고 접수
     */
    public void createReport(Long userId, Long propertyId, ReportCreateReqDto reqDto){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new CustomException(UserErrorCode.USER_NOT_FOUND));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        /*
         * 내가 등록한 매물 신고 불가
         */
        if (property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.CANNOT_REPORT_OWN_PROPERTY);
        }

        /*
         * 중복 신고 방지
         */
        if (reportRepository.existsByPropertyAndReporter(property, user)) {
            throw new CustomException(PropertyErrorCode.ALREADY_REPORTED);
        }

        /*
         * 기타 사유(OTHER)일 때 사용
         */
        String savedCustomReason = null;
        if (reqDto.reason() == ReportReason.OTHER) {
            savedCustomReason = reqDto.customReason();
        }

        /*
         * 저장
         */
        try {
            PropertyReport report = PropertyReport.builder()
                    .reason(reqDto.reason())
                    .customReason(savedCustomReason)
                    .property(property)
                    .reporter(user)
                    .build();
            reportRepository.save(report);

            reportRepository.flush();

            propertyRepository.increaseReportCountAndCheckSuspicious(propertyId);

        } catch (DataIntegrityViolationException e) {
            throw new CustomException(PropertyErrorCode.ALREADY_REPORTED);
        }
    }


    /**
     * 내 신고 내역 조회
     */
    @Transactional(readOnly = true)
    public List<ReportListRespDto> getMyReports(Long userId) {
        return reportRepository.findAllByReporterId(userId).stream()
                .map(ReportListRespDto::from)
                .toList();
    }

    /**
     * 특정 매물의 누적 신고 횟수 조회
     *
     * @param propertyId 매물 ID
     * @return 매물 신고 요약
     */
    @Transactional(readOnly = true)
    public PropertyReportSummaryResDto getReportSummary(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        return PropertyReportSummaryResDto.from(property);
    }
}
