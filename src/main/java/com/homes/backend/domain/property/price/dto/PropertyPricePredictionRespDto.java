package com.homes.backend.domain.property.price.dto;

import com.homes.backend.domain.property.entity.TradeType;
import com.homes.backend.domain.property.price.model.PricePrediction;
import com.homes.backend.domain.property.price.model.PricePredictionConfidence;
import com.homes.backend.domain.property.price.model.PricePredictionStatus;
import com.homes.backend.domain.property.price.model.ComparisonScope;
import com.homes.backend.domain.property.price.model.ComparableTradeSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;
import java.util.List;

public record PropertyPricePredictionRespDto(
        Long propertyId,
        PricePredictionStatus status,
        TradeType tradeType,
        @Schema(description = "예상 거래가(만원)", example = "63500") Long predictedPrice,
        @Schema(description = "예상 가격 하한(만원)", example = "60000") Long minimumPrice,
        @Schema(description = "예상 가격 상한(만원)", example = "67000") Long maximumPrice,
        PricePredictionConfidence confidence,
        ComparisonScope comparisonScope,
        int sampleCount,
        YearMonth referenceFrom,
        YearMonth referenceTo,
        List<ComparableTradeSummary> representativeTrades,
        String method,
        String description
) {
    public static PropertyPricePredictionRespDto from(Long propertyId, TradeType tradeType, PricePrediction prediction) {
        return new PropertyPricePredictionRespDto(propertyId, prediction.status(), tradeType,
                prediction.predictedPrice(), prediction.minimumPrice(), prediction.maximumPrice(),
                prediction.confidence(), prediction.comparisonScope(), prediction.sampleCount(),
                prediction.referenceFrom(), prediction.referenceTo(), prediction.representativeTrades(),
                prediction.method(), prediction.description());
    }
}
