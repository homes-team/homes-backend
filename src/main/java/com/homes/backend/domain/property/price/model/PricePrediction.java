package com.homes.backend.domain.property.price.model;

import java.time.YearMonth;

public record PricePrediction(
        PricePredictionStatus status,
        Long predictedPrice,
        Long minimumPrice,
        Long maximumPrice,
        PricePredictionConfidence confidence,
        int sampleCount,
        YearMonth referenceFrom,
        YearMonth referenceTo,
        String method,
        String description
) {
    public static PricePrediction unavailable(PricePredictionStatus status, String description) {
        return new PricePrediction(status, null, null, null, PricePredictionConfidence.UNAVAILABLE,
                0, null, null, PricePredictionPolicy.METHOD, description);
    }
}
