package com.homes.backend.domain.review.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "리뷰 작성 요청 DTO")
public record ReviewCreateReqDto(
        @Schema(description = "평점 (0.0 ~ 5.0)", example = "4.5")
        @NotNull(message = "평점은 필수입니다.")
        @DecimalMin(value = "0.0", message = "평점은 0.0 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다.")
        Float score,

        @Schema(description = "리뷰 내용", example = "친절하고 응대가 빠르셨어요.")
        @Size(max = 500, message = "리뷰 내용은 500자를 초과할 수 없습니다.")
        String content
) {}
