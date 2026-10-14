package com.homes.backend.domain.verification.controller;

import com.homes.backend.domain.verification.dto.request.OwnerVerificationReqDto;
import com.homes.backend.domain.verification.dto.request.RealtorVerificationReqDto;
import com.homes.backend.domain.verification.dto.response.VerificationStatusRespDto;
import com.homes.backend.domain.verification.entity.VerificationStatus;
import com.homes.backend.domain.verification.exception.VerificationErrorCode;
import com.homes.backend.domain.verification.service.OwnerVerificationService;
import com.homes.backend.domain.verification.service.RealtorVerificationService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import com.homes.backend.global.util.ExifData;
import com.homes.backend.global.util.ExifExtractor;
import com.homes.backend.global.util.ImageDownloadUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties/{propertyId}/verifications")
public class VerificationController implements VerificationControllerDocs {
    private final RealtorVerificationService realtorVerificationService;
    private final ExifExtractor exifExtractor;
    private final ImageDownloadUtil imageDownloadUtil;
    private final OwnerVerificationService ownerVerificationService;

    @Override
    @PostMapping
    public ApiResponse<String> requestRealtorVerification(
            @PathVariable Long propertyId,
            @Valid @RequestBody RealtorVerificationReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();

        // URL 유효성(SSRF 방어) 검증
        String photoUrl = reqDto.photoUrl();
        if (photoUrl == null || (!photoUrl.startsWith("http://") && !photoUrl.startsWith("https://"))) {
            throw new CustomException(VerificationErrorCode.INVALID_IMAGE_URL);
        }

        // SSRF 방어 및 용량 제한이 적용된 안전한 다운로드 실행 (단 1회만 다운로드)
        byte[] downloadedImageBytes = imageDownloadUtil.downloadImageSecurely(photoUrl);

        // 다운로드 된 바이트 배열을 넘겨서 EXIF 추출
        ExifData exifData = exifExtractor.extractExif(downloadedImageBytes);
        if (exifData == null) {
            throw new CustomException(VerificationErrorCode.EXIF_NOT_FOUND);
        }

        // 검증 로직 실행
        VerificationStatus status = realtorVerificationService.verifyOnSite(propertyId, userId, reqDto, exifData, downloadedImageBytes);

        // 100m 초과로 반려된 경우 (중개사 위치)
        if (status == VerificationStatus.REJECTED) {
            throw new CustomException(VerificationErrorCode.REALTOR_GPS_NOT_MATCH);
        }

        // 100m 이내 승인 및 도용 검사 모두 통과 시 성공
        return ApiResponse.onSuccess("현장 인증이 성공적으로 처리되었습니다.");
    }

    @Override
    @GetMapping
    public ApiResponse<VerificationStatusRespDto> getVerificationStatus(
            @PathVariable Long propertyId
    ) {
        VerificationStatusRespDto response = realtorVerificationService.getVerificationStatus(propertyId);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @PostMapping("/owner")
    public ApiResponse<String> requestOwnerVerification(
            @PathVariable Long propertyId,
            @Valid @RequestBody OwnerVerificationReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        // 집주인 서류 인증 요청 (비동기로 돌아가므로 곧바로 리턴됨)
        ownerVerificationService.requestOwnerVerification(propertyId, userPrincipal.getId(), reqDto);

        // 프론트엔드에 요청 성공 응답만 먼저 보냄
        return ApiResponse.onSuccess("집주인 서류 인증이 요청되었습니다. 1~2분 후 상태를 확인해주세요.");
    }
}