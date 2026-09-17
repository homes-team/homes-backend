package com.homes.backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AdminPropertySuspiciousReqDto(
        @Schema(description = "의심 매물 지정 여부", example = "true")
        @NotNull(message = "isSuspicious는 필수입니다.")
        Boolean isSuspicious
) {
}
