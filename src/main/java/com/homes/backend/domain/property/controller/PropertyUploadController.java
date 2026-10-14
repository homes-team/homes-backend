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
import org.springframework.util.StringUtils;
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
    public ApiResponse<PresignedUrlResDto> getPresignedUrl(
            @RequestParam String fileName,
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request
    ) {
        checkRateLimit(request.getRemoteAddr());

        String identity = resolveIdentity(userPrincipal, email);
        PresignedUploadInfo info = s3PresignedUrlService.issueUploadUrl(UPLOAD_FOLDER, fileName, identity);
        return ApiResponse.onSuccess(PresignedUrlResDto.from(info));
    }

    /**
     * 로그인한 유저(매물 사진)는 userId로, 아직 계정이 없는 중개사 가입 지원자(서류 사진)는
     * 이메일 인증을 마친 email로 발급 주체를 식별한다. 나중에 실제 제출 시 이 식별자와 대조한다.
     */
    private String resolveIdentity(UserPrincipal userPrincipal, String email) {
        if (userPrincipal != null) {
            return "user:" + userPrincipal.getId();
        }

        if (StringUtils.hasText(email)) {
            Object isVerified = redisTemplate.opsForValue().get("AUTH_SUCCESS:" + email);
            if ("TRUE".equals(isVerified)) {
                return "email:" + email;
            }
        }

        throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
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
