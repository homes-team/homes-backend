package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.response.PresignedUrlResDto;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.storage.PresignedUploadInfo;
import com.homes.backend.global.storage.S3PresignedUrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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

    // 인증 없이 호출 가능한 엔드포인트라 남용 방지용으로 IP당 발급 빈도를 제한한다 (분당 30회)
    private static final int RATE_LIMIT_MAX_REQUESTS = 30;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final S3PresignedUrlService s3PresignedUrlService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlResDto> getPresignedUrl(@RequestParam String fileName, HttpServletRequest request) {
        checkRateLimit(request.getRemoteAddr());

        PresignedUploadInfo info = s3PresignedUrlService.issueUploadUrl(UPLOAD_FOLDER, fileName);
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
