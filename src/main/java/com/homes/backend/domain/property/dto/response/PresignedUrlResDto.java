package com.homes.backend.domain.property.dto.response;

import com.homes.backend.global.storage.PresignedUploadInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 업로드용 Presigned URL 발급 응답 DTO")
public record PresignedUrlResDto(
        @Schema(description = "클라이언트가 파일 바이트를 PUT으로 업로드할 임시 서명 URL (5분간 유효)")
        String uploadUrl,

        @Schema(description = "업로드 완료 후 실제로 저장될 최종 공개 URL. 매물 등록 시 이 값을 사용한다.")
        String fileUrl
) {
    public static PresignedUrlResDto from(PresignedUploadInfo info) {
        return new PresignedUrlResDto(info.uploadUrl(), info.fileUrl());
    }
}
