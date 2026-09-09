package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.response.PresignedUrlResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "매물 이미지 업로드(Property Upload) API", description = "S3 Presigned URL 발급 API")
public interface PropertyUploadControllerDocs {

    @Operation(summary = "이미지 업로드용 Presigned URL 발급", description = "사용자가 집 사진을 올릴 때 서버 부하를 줄이기 위해, " +
            "AWS S3에 클라이언트가 직접 사진을 올릴 수 있는 임시 권한 URL을 발급합니다. " +
            "응답의 uploadUrl로 파일을 직접 PUT 업로드하고, fileUrl은 매물 등록 요청에 사용합니다. " +
            "익명 호출도 허용되는 엔드포인트라 남용 방지를 위해 IP당 분당 발급 횟수를 제한합니다.")
    ApiResponse<PresignedUrlResDto> getPresignedUrl(
            @Parameter(description = "업로드할 원본 파일명 (확장자 추출용)", example = "photo.jpg", required = true)
            @RequestParam String fileName,
            @Parameter(hidden = true) HttpServletRequest request
    );
}
