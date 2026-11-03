package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.response.PresignedUrlResDto;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import com.homes.backend.global.storage.PresignedUploadInfo;
import com.homes.backend.global.storage.S3PresignedUrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyUploadController implements PropertyUploadControllerDocs {

    private static final String UPLOAD_FOLDER = "properties";

    // 인증 없이도 호출 가능한 엔드포인트(로그인 전 상태에서의 호출은 없지만 혹시 몰라 방어)라 남용 방지용으로 IP당 발급 빈도를 제한한다 (분당 30회)
    private static final int RATE_LIMIT_MAX_REQUESTS = 30;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final S3PresignedUrlService s3PresignedUrlService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlResDto> getPresignedUrl(
            @RequestParam String fileName,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request
    ) {
        checkRateLimit(request.getRemoteAddr());

        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        String identity = "user:" + userPrincipal.getId();
        PresignedUploadInfo info = s3PresignedUrlService.issueUploadUrl(UPLOAD_FOLDER, fileName, identity);
        return ApiResponse.onSuccess(PresignedUrlResDto.from(info));
    }

    private void checkRateLimit(String clientIp) {
        String key = "PRESIGN_RATE:" + clientIp;
        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1L) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW);
        }

        if (requestCount != null && requestCount > RATE_LIMIT_MAX_REQUESTS) {
            throw new CustomException(GlobalErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
