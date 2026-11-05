package com.homes.backend.domain.verification.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.realtor.exception.RealtorErrorCode;
import com.homes.backend.domain.realtor.repository.AgentRepository;
import com.homes.backend.domain.verification.dto.request.RealtorVerificationReqDto;
import com.homes.backend.domain.verification.dto.response.VerificationStatusRespDto;
import com.homes.backend.domain.verification.entity.OwnerVerification;
import com.homes.backend.domain.verification.entity.RealtorVerification;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.domain.verification.repository.OwnerVerificationRepository;
import com.homes.backend.domain.verification.repository.RealtorVerificationRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.storage.S3UploadService;
import com.homes.backend.global.util.ExifData;
import com.homes.backend.global.util.VisionApiService;
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

    private final S3UploadService s3UploadService;
    private final PropertyRepository propertyRepository;
    private final AgentRepository agentRepository;
    private final RealtorVerificationRepository realtorVerificationRepository;
    private final OwnerVerificationRepository ownerVerificationRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double MAX_ALLOWED_DISTANCE_METERS = 100.0;
    private final VisionApiService visionApiService;

    @Transactional
    public VerificationStatus verifyOnSite(Long propertyId, Long userId, RealtorVerificationReqDto reqDto, ExifData exifData, byte[] imageBytes) {
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

        // 과거
        if (exifData.originalDate().isBefore(oneHourAgo)) {
            throw new CustomException(VerificationErrorCode.EXIF_TIME_EXPIRED);
        }

        // 미래
        if (exifData.originalDate().isAfter(fiveMinsLater)) {
            throw new CustomException(VerificationErrorCode.EXIF_TIME_FUTURE);
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

        VerificationStatus status = (distanceMeter != null && distanceMeter <= MAX_ALLOWED_DISTANCE_METERS)
                ? VerificationStatus.APPROVED
                : VerificationStatus.REJECTED;

        // 최종적으로 DB에 저장될 안전한 URL 변수 선언 (기본값은 프론트에서 보낸 URL)
        String securePhotoUrl = reqDto.photoUrl();

        // --- 도용 사진(역이미지 검색) 검사 (거리 검증을 통과한 경우에만 수행) ---
        if (status == VerificationStatus.APPROVED) {
            boolean isStolen = visionApiService.isStolenImage(imageBytes);
            if (isStolen) {
                // 도용이 확인되면 에러를 던짐
                throw new CustomException(VerificationErrorCode.STOLEN_IMAGE_DETECTED);
            }

            // [보안 취약점(TOCTOU) 방어 로직 추가]
            // 검증(도용, EXIF 등)을 모두 통과한 1차 다운로드된 안전한 원본 byte[]를 S3 격리 폴더에 직접 업로드합니다.
            // 남은 Presigned URL 유효 시간 동안 악의적인 덮어쓰기 공격이 들어오더라도 DB에는 이 새로운 URL이 저장되므로 안전합니다.
            securePhotoUrl = s3UploadService.uploadVerifiedImage(imageBytes, reqDto.photoUrl());
        }

        RealtorVerification verification = RealtorVerification.builder()
                .property(property)
                .agent(agent)
                .photoUrl(securePhotoUrl) // 프론트엔드 URL 대신, 백엔드가 직접 업로드한 안전한 URL 저장
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

        // 중개사 현장 인증 상태 조회
        RealtorVerification latestVerification = realtorVerificationRepository
                .findTopByPropertyIdOrderByRequestedAtDesc(propertyId)
                .orElse(null);

        boolean isRealtorVerified = (latestVerification != null && latestVerification.getStatus() == VerificationStatus.APPROVED);
        VerificationStatus realtorStatus = (latestVerification != null) ? latestVerification.getStatus() : null;

        // 집주인 서류 인증 상태 조회
        OwnerVerification latestOwner = ownerVerificationRepository
                .findTopByPropertyIdOrderByRequestedAtDesc(propertyId)
                .orElse(null);
        boolean isOwnerVerified = (latestOwner != null && latestOwner.getStatus() == VerificationStatus.APPROVED);

        // 집주인 서류 인증 상태 상세 조회
        VerificationStatus ownerStatus = (latestOwner != null) ? latestOwner.getStatus() : null;

        return new VerificationStatusRespDto(isOwnerVerified, ownerStatus, isRealtorVerified, realtorStatus);
    }
}