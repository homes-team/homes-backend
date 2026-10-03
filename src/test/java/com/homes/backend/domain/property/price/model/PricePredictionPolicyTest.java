package com.homes.backend.domain.property.price.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricePredictionPolicyTest {
    private final PricePredictionPolicy policy = new PricePredictionPolicy();
    private final LocalDate asOf = LocalDate.of(2026, 10, 1);
    private final PricePredictionTarget target = new PricePredictionTarget(84.0, 10, 2015, "홈즈아파트");

    @Test
    void predictsWeightedMedianAndRangeFromComparableTrades() {
        PricePrediction result = policy.predict(target, List.of(
                trade("홈즈아파트", 83.5, 9, 2015, 60_000, asOf.minusMonths(1)),
                trade("홈즈아파트", 84.2, 11, 2016, 62_000, asOf.minusMonths(2)),
                trade("이웃아파트", 82.0, 8, 2014, 59_000, asOf.minusMonths(3)),
                trade("이웃아파트", 85.0, 12, 2017, 63_000, asOf.minusMonths(4))
        ), asOf);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.AVAILABLE);
        assertThat(result.predictedPrice()).isBetween(59_000L, 63_000L);
        assertThat(result.minimumPrice()).isLessThanOrEqualTo(result.predictedPrice());
        assertThat(result.maximumPrice()).isGreaterThanOrEqualTo(result.predictedPrice());
        assertThat(result.sampleCount()).isEqualTo(4);
        assertThat(result.confidence()).isEqualTo(PricePredictionConfidence.LOW);
    }

    @Test
    void excludesExtremePricePerAreaOutlier() {
        PricePrediction result = policy.predict(target, List.of(
                trade("홈즈아파트", 84, 10, 2015, 60_000, asOf.minusMonths(1)),
                trade("홈즈아파트", 84, 10, 2015, 61_000, asOf.minusMonths(2)),
                trade("홈즈아파트", 84, 10, 2015, 62_000, asOf.minusMonths(3)),
                trade("홈즈아파트", 84, 10, 2015, 63_000, asOf.minusMonths(4)),
                trade("홈즈아파트", 84, 10, 2015, 300_000, asOf.minusMonths(1))
        ), asOf);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.AVAILABLE);
        assertThat(result.sampleCount()).isEqualTo(4);
        assertThat(result.predictedPrice()).isLessThan(70_000L);
    }

    @Test
    void relaxesAreaAndBuildingYearWhenStrictSamplesAreInsufficient() {
        PricePrediction result = policy.predict(target, List.of(
                trade("A", 70, 3, 2007, 48_000, asOf.minusMonths(1)),
                trade("B", 72, 5, 2008, 50_000, asOf.minusMonths(2)),
                trade("C", 100, 15, 2023, 72_000, asOf.minusMonths(3))
        ), asOf);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.AVAILABLE);
        assertThat(result.sampleCount()).isEqualTo(3);
    }

    @Test
    void returnsInsufficientDataWhenFewerThanThreeComparablesRemain() {
        PricePrediction result = policy.predict(target, List.of(
                trade("홈즈아파트", 84, 10, 2015, 60_000, asOf.minusMonths(1)),
                trade("홈즈아파트", 84, 11, 2015, 61_000, asOf.minusMonths(2))
        ), asOf);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.INSUFFICIENT_DATA);
        assertThat(result.predictedPrice()).isNull();
        assertThat(result.confidence()).isEqualTo(PricePredictionConfidence.UNAVAILABLE);
    }

    @Test
    void ignoresFutureAndOlderThanTwentyFourMonthTrades() {
        PricePrediction result = policy.predict(target, List.of(
                trade("A", 84, 10, 2015, 60_000, asOf.plusDays(1)),
                trade("B", 84, 10, 2015, 60_000, asOf.minusMonths(25)),
                trade("C", 84, 10, 2015, 60_000, asOf.minusMonths(1))
        ), asOf);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.INSUFFICIENT_DATA);
    }

    private ApartmentTrade trade(String name, double area, int floor, Integer buildYear, long price, LocalDate date) {
        return new ApartmentTrade(name, "역삼동", "123", area, floor, buildYear, price, date);
    }
}
