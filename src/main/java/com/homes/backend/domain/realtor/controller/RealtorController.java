package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.property.dto.response.PresignedUrlResDto;
import com.homes.backend.domain.realtor.dto.request.RealtorSignupReqDto;
import com.homes.backend.domain.realtor.dto.response.RealtorSignupResDto;
import com.homes.backend.domain.realtor.service.RealtorService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.storage.PresignedUploadInfo;
import com.homes.backend.global.storage.S3PresignedUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class RealtorController implements RealtorControllerDocs {

    private static final String UPLOAD_FOLDER = "agents";

    // 로그인 전(계정 없는) 가입 지원자도 호출하는 엔드포인트라 남용 방지용으로 IP당 발급 빈도를 제한한다 (분당 30회)
    private static final int RATE_LIMIT_MAX_REQUESTS = 30;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final RealtorService realtorService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 이미 프론트에 연동된 기존 경로라 /users/realtors로 유지 (presigned-url만 /realtors로 통일됨 - 비일관적이지만 깨지는 것보단 안전)
    @Override
    @PostMapping("/users/realtors")
    public ApiResponse<RealtorSignupResDto> signUpRealtor(@RequestBody @Valid RealtorSignupReqDto request) {
        RealtorSignupResDto response = realtorService.signUp(request);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @GetMapping("/realtors/presigned-url")
    public ApiResponse<PresignedUrlResDto> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam String email,
            HttpServletRequest request
    ) {
        checkRateLimit(request.getRemoteAddr());

        Object isVerified = redisTemplate.opsForValue().get("AUTH_SUCCESS:" + email);
        if (!StringUtils.hasText(email) || !"TRUE".equals(isVerified)) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        String identity = "email:" + email;
        PresignedUploadInfo info = s3PresignedUrlService.issueUploadUrl(UPLOAD_FOLDER, fileName, identity);
        return ApiResponse.onSuccess(PresignedUrlResDto.from(info));
    }

    private void checkRateLimit(String clientIp) {
        String key = "PRESIGN_RATE:AGENT:" + clientIp;
        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1L) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW);
        }

        if (requestCount != null && requestCount > RATE_LIMIT_MAX_REQUESTS) {
            throw new CustomException(GlobalErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
