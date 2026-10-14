package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "매물 추천 API", description = "AI, 찜하기, 최근 본 방 기반 추천 매물 조회 API")
public interface PropertyRecommendationControllerDocs {
    @Operation(summary = "하이브리드 맞춤 추천 매물 조회", description = "AI 점수(50%), 찜하기(45%), 최근 본 방 보너스(5%)를 결합하여 맞춤 매물을 추천합니다.")
    ApiResponse<List<PropertyListRespDto>> getHybridRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "최소 보증금 (선택)") Double minPrice,
            @Parameter(description = "최대 보증금 (선택)") Double maxPrice,
            @Parameter(description = "선호 지역 (선택, 예: '강남구')") String preferredRegion
    );
}
