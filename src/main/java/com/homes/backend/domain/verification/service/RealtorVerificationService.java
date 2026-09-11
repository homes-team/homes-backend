package com.homes.backend.domain.verification.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.exception.RealtorErrorCode;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.domain.verification.dto.request.RealtorVerificationReqDto;
import com.homes.backend.domain.verification.dto.response.VerificationStatusRespDto;
import com.homes.backend.domain.verification.entity.RealtorVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.domain.verification.repository.RealtorVerificationRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.util.ExifData;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class RealtorVerificationService {

    private final PropertyRepository propertyRepository;
    private final AgentRepository agentRepository;
    private final RealtorVerificationRepository realtorVerificationRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double MAX_ALLOWED_DISTANCE_METERS = 100.0;

    @Transactional
    public VerificationStatus verifyOnSite(Long propertyId, Long userId, RealtorVerificationReqDto reqDto, ExifData exifData) {
        Property property = propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        Agent agent = agentRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(RealtorErrorCode.AGENT_NOT_FOUND));

        // 공인중개사가 승인 안된 사람일 시 오류 처리
        if (!agent.isVerified()) {
            throw new CustomException(RealtorErrorCode.UNAPPROVED_AGENT);
        }

        RealtorVerification latestVerification = realtorVerificationRepository
                .findTopByPropertyIdOrderByRequestedAtDesc(propertyId)
                .orElse(null);

        // 미승인된 매물만 인증 처리
        if (latestVerification != null && latestVerification.getStatus() == VerificationStatus.APPROVED) {
            throw new CustomException(VerificationErrorCode.ALREADY_VERIFIED);
        }

        // --- 촬영 시각 검증 (과거 1시간 ~ 미래 5분 허용) ---
        if (exifData.originalDate() == null) {
            throw new CustomException(VerificationErrorCode.EXIF_TIME_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime fiveMinsLater = now.plusMinutes(5); // 스마트폰 기기 간 시간 오차 5분 허용

        if (exifData.originalDate().isBefore(oneHourAgo)) {
            throw new CustomException(VerificationErrorCode.EXIF_TIME_EXPIRED);
        }

        // --- 사진에 기록된 GPS 기반 거리 검증 ---
        Point photoLocation = geometryFactory.createPoint(new Coordinate(exifData.longitude(), exifData.latitude()));
        Double photoDistance = propertyRepository.calculateDistanceToProperty(propertyId, photoLocation);

        // 100m 초과일시 에러
        if (photoDistance == null || photoDistance > MAX_ALLOWED_DISTANCE_METERS) {
            throw new CustomException(VerificationErrorCode.EXIF_GPS_NOT_MATCH);
        }

        // --- 중개사가 현재 서 있는 위치(휴대폰 GPS) 기반 거리 계산 ---
        Point realtorLocation = geometryFactory.createPoint(new Coordinate(reqDto.longitude(), reqDto.latitude()));
        Double distanceMeter = propertyRepository.calculateDistanceToProperty(propertyId, realtorLocation);

        // TODO: EXIF 데이터는 위조가 가능하므로(CWE-345), 추후 Google Vision API(역이미지 검색)를 도입하여
        // 2차 검증을 수행하거나 MANUAL_REVIEW 상태로 전환하는 로직을 고도화할 예정.
        VerificationStatus status = (distanceMeter != null && distanceMeter <= MAX_ALLOWED_DISTANCE_METERS)
                ? VerificationStatus.APPROVED
                : VerificationStatus.REJECTED;

        RealtorVerification verification = RealtorVerification.builder()
                .property(property)
                .agent(agent)
                .photoUrl(reqDto.photoUrl())
                .location(realtorLocation)
                .distanceMeter(distanceMeter)
                .status(status)
                .build();

        realtorVerificationRepository.save(verification);

        return status;
    }

    @Transactional(readOnly = true)
    public VerificationStatusRespDto getVerificationStatus(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND);
        }

        RealtorVerification latestVerification = realtorVerificationRepository
                .findTopByPropertyIdOrderByRequestedAtDesc(propertyId)
                .orElse(null);

        boolean isRealtorVerified = (latestVerification != null && latestVerification.getStatus() == VerificationStatus.APPROVED);
        VerificationStatus realtorStatus = (latestVerification != null) ? latestVerification.getStatus() : null;

        return new VerificationStatusRespDto(false, isRealtorVerified, realtorStatus);
    }
}