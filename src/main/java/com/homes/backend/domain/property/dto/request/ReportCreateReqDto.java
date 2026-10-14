package com.homes.backend.domain.property.dto.request;

import com.homes.backend.domain.property.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "매물 신고 요청 DTO")
public record ReportCreateReqDto(
        @Schema(description = "신고 사유 선택 (FAKE_PROPERTY, SOLD_OUT, PRICE_MISMATCH, INFO_MISMATCH, OTHER)", example = "OTHER")
        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,

        @Schema(description = "기타(OTHER) 사유 선택 시 직접 입력하는 내용", example = "집주인이 연락을 계속 피합니다.")
        String customReason
) {
}
