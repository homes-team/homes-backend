package com.homes.backend.domain.property.insight.controller;

import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto;
import com.homes.backend.domain.property.insight.dto.IsochroneRespDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "매물 AI·생활권 API", description = "매물 다면평가와 생활권 등시선도를 조회합니다.")
public interface PropertyInsightControllerDocs {
    /**
     * 매물의 AI 다면평가를 조회합니다.
     *
     * @param propertyId 매물 ID
     * @return AI 평가 응답
     */
    @Operation(summary = "AI 매물 다면평가 조회", description = "100점 원본 점수와 5점 표시 점수, 6개 평가 항목 및 구조화된 리포트를 반환합니다.")
    ApiResponse<AiEvaluationRespDto> getAiEvaluation(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId
    );

    /**
     * 이동수단과 이동시간에 따른 매물의 생활권을 조회합니다.
     *
     * @param propertyId 매물 ID
     * @param mode 이동수단
     * @param travelTimeMinutes 이동시간(분)
     * @return GeoJSON 등시선도 응답
     */
    @Operation(summary = "생활권 Isochrone 조회", description = "도보/차량 및 이동시간에 따른 GeoJSON Polygon을 반환합니다.")
    ApiResponse<IsochroneRespDto> getIsochrone(
            @Parameter(description = "매물 ID") @PathVariable Long propertyId,
            @Parameter(description = "walk 또는 drive", example = "walk") @RequestParam String mode,
            @Parameter(description = "5, 10, 15, 20, 30", example = "10") @RequestParam(name = "time") int travelTimeMinutes
    );
}
