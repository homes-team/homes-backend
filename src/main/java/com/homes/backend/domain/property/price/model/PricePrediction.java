package com.homes.backend.domain.property.price.model;

import java.time.YearMonth;
import java.util.List;

public record PricePrediction(
        PricePredictionStatus status,
        Long predictedPrice,
        Long minimumPrice,
        Long maximumPrice,
        PricePredictionConfidence confidence,
        ComparisonScope comparisonScope,
        int sampleCount,
        YearMonth referenceFrom,
        YearMonth referenceTo,
        List<ComparableTradeSummary> representativeTrades,
        String method,
        String description
) {
    public static PricePrediction unavailable(PricePredictionStatus status, String description) {
        return new PricePrediction(status, null, null, null, PricePredictionConfidence.UNAVAILABLE,
                ComparisonScope.UNAVAILABLE, 0, null, null, List.of(), PricePredictionPolicy.METHOD, description);
    }
}
