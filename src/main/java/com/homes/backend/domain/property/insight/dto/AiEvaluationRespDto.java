package com.homes.backend.domain.property.insight.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiEvaluationRespDto(
        Long propertyId,
        OverallScore overall,
        List<CategoryScore> categories,
        Report report,
        String scoreVersion,
        String reportModelVersion,
        LocalDateTime generatedAt
) {
    public record OverallScore(
            Double rawScore,
            Double displayScore,
            int evaluatedCategoryCount,
            int totalCategoryCount,
            double completeness
    ) {}

    public record CategoryScore(
            CategoryKey key,
            String label,
            Double rawScore,
            Double displayScore,
            ScoreStatus status,
            ScoreSource source,
            String description
    ) {}

    public record Report(
            String summary,
            List<String> strengths,
            List<String> weaknesses,
            String notice
    ) {}

    public enum CategoryKey { SCHOOL, TRANSPORT, NATURE, SUNLIGHT, BUILDING_CONDITION, INFRASTRUCTURE }
    public enum ScoreStatus { AVAILABLE, PENDING_DATA, INSUFFICIENT_DATA, NOT_APPLICABLE }
    public enum ScoreSource { GEOSPATIAL_PIPELINE, PROPERTY_RULE, EXTERNAL_DATA, MANUAL, NONE }
}
