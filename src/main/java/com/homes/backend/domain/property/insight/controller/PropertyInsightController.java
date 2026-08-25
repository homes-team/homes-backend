package com.homes.backend.domain.property.insight.controller;

import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto;
import com.homes.backend.domain.property.insight.dto.IsochroneRespDto;
import com.homes.backend.domain.property.insight.service.PropertyInsightService;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties/{propertyId}")
public class PropertyInsightController implements PropertyInsightControllerDocs {
    private final PropertyInsightService propertyInsightService;

    @Override
    @GetMapping("/ai-evaluation")
    public ApiResponse<AiEvaluationRespDto> getAiEvaluation(@PathVariable Long propertyId) {
        return ApiResponse.onSuccess(propertyInsightService.getAiEvaluation(propertyId));
    }

    @Override
    @GetMapping("/isochrone")
    public ApiResponse<IsochroneRespDto> getIsochrone(
            @PathVariable Long propertyId,
            @RequestParam String mode,
            @RequestParam(name = "time") int travelTimeMinutes
    ) {
        return ApiResponse.onSuccess(propertyInsightService.getIsochrone(propertyId, mode, travelTimeMinutes));
    }
}
