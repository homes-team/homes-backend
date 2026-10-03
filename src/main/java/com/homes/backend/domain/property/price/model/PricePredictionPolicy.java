package com.homes.backend.domain.property.price.model;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class PricePredictionPolicy {
    public static final String METHOD = "COMPARABLE_WEIGHTED_MEDIAN_V1";
    private static final int MINIMUM_SAMPLES = 3;

    public PricePrediction predict(PricePredictionTarget target, List<ApartmentTrade> source, LocalDate asOf) {
        List<ApartmentTrade> candidates = source.stream()
                .filter(trade -> trade.areaSquareMeters() > 0 && trade.priceTenThousandWon() > 0)
                .filter(trade -> !trade.contractDate().isAfter(asOf))
                .filter(trade -> !trade.contractDate().isBefore(asOf.minusMonths(24)))
                .toList();

        List<ApartmentTrade> selected = select(candidates, target, 0.15, 5);
        if (selected.size() < MINIMUM_SAMPLES) {
            selected = select(candidates, target, 0.25, 10);
        }
        selected = removeOutliers(selected);
        if (selected.size() < MINIMUM_SAMPLES) {
            return PricePrediction.unavailable(PricePredictionStatus.INSUFFICIENT_DATA,
                    "가격을 예측하기 위한 유사 거래가 부족합니다.");
        }

        List<WeightedValue> values = selected.stream()
                .map(trade -> new WeightedValue(pricePerSquareMeter(trade), weight(trade, target, asOf)))
                .sorted(Comparator.comparingDouble(WeightedValue::value))
                .toList();
        double basePerSquareMeter = weightedPercentile(values, 0.5);
        double trendFactor = trendFactor(selected, asOf);
        long predicted = roundPrice(basePerSquareMeter * target.areaSquareMeters() * trendFactor);

        long q1 = roundPrice(weightedPercentile(values, 0.25) * target.areaSquareMeters() * trendFactor);
        long q3 = roundPrice(weightedPercentile(values, 0.75) * target.areaSquareMeters() * trendFactor);
        long minimum = Math.min(q1, roundPrice(predicted * 0.95));
        long maximum = Math.max(q3, roundPrice(predicted * 1.05));

        YearMonth from = selected.stream().map(ApartmentTrade::contractDate).min(LocalDate::compareTo).map(YearMonth::from).orElse(null);
        YearMonth to = selected.stream().map(ApartmentTrade::contractDate).max(LocalDate::compareTo).map(YearMonth::from).orElse(null);
        long sameApartmentCount = selected.stream().filter(trade -> sameApartment(trade.apartmentName(), target.apartmentName())).count();
        PricePredictionConfidence confidence = confidence(selected.size(), sameApartmentCount);
        String description = "최근 유사 아파트 거래 %d건을 기준으로 계산했습니다.".formatted(selected.size());
        return new PricePrediction(PricePredictionStatus.AVAILABLE, predicted, minimum, maximum, confidence,
                selected.size(), from, to, METHOD, description);
    }

    private List<ApartmentTrade> select(List<ApartmentTrade> source, PricePredictionTarget target,
                                        double areaTolerance, int buildingYearTolerance) {
        return source.stream()
                .filter(trade -> Math.abs(trade.areaSquareMeters() - target.areaSquareMeters()) / target.areaSquareMeters() <= areaTolerance)
                .filter(trade -> target.buildYear() == null || trade.buildYear() == null
                        || Math.abs(trade.buildYear() - target.buildYear()) <= buildingYearTolerance)
                .toList();
    }

    private List<ApartmentTrade> removeOutliers(List<ApartmentTrade> trades) {
        if (trades.size() < 5) return trades;
        List<Double> prices = trades.stream().map(PricePredictionPolicy::pricePerSquareMeter).sorted().toList();
        double median = percentile(prices, 0.5);
        return trades.stream()
                .filter(trade -> {
                    double value = pricePerSquareMeter(trade);
                    return value >= median * 0.5 && value <= median * 1.5;
                })
                .toList();
    }

    private double weight(ApartmentTrade trade, PricePredictionTarget target, LocalDate asOf) {
        long monthsOld = Math.max(0, ChronoUnit.MONTHS.between(YearMonth.from(trade.contractDate()), YearMonth.from(asOf)));
        double recency = 1.0 / (1.0 + monthsOld / 6.0);
        double areaDifference = Math.abs(trade.areaSquareMeters() - target.areaSquareMeters()) / target.areaSquareMeters();
        double area = 1.0 / (1.0 + areaDifference * 4.0);
        double floor = 1.0 / (1.0 + Math.abs(trade.floor() - target.floor()) / 10.0);
        double year = target.buildYear() == null || trade.buildYear() == null
                ? 0.9 : 1.0 / (1.0 + Math.abs(trade.buildYear() - target.buildYear()) / 10.0);
        double apartment = sameApartment(trade.apartmentName(), target.apartmentName()) ? 2.0 : 1.0;
        return recency * area * floor * year * apartment;
    }

    private double trendFactor(List<ApartmentTrade> trades, LocalDate asOf) {
        LocalDate recentFrom = asOf.minusMonths(6);
        LocalDate previousFrom = asOf.minusMonths(12);
        List<Double> recent = trades.stream().filter(t -> !t.contractDate().isBefore(recentFrom))
                .map(PricePredictionPolicy::pricePerSquareMeter).sorted().toList();
        List<Double> previous = trades.stream().filter(t -> !t.contractDate().isBefore(previousFrom) && t.contractDate().isBefore(recentFrom))
                .map(PricePredictionPolicy::pricePerSquareMeter).sorted().toList();
        if (recent.size() < 2 || previous.size() < 2) return 1.0;
        double change = percentile(recent, 0.5) / percentile(previous, 0.5) - 1.0;
        return 1.0 + Math.max(-0.05, Math.min(0.05, change * 0.5));
    }

    private static double weightedPercentile(List<WeightedValue> values, double percentile) {
        double total = values.stream().mapToDouble(WeightedValue::weight).sum();
        double threshold = total * percentile;
        double cumulative = 0;
        for (WeightedValue value : values) {
            cumulative += value.weight();
            if (cumulative >= threshold) return value.value();
        }
        return values.get(values.size() - 1).value();
    }

    private static double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.floor((sorted.size() - 1) * percentile);
        return sorted.get(index);
    }

    private static double pricePerSquareMeter(ApartmentTrade trade) {
        return trade.priceTenThousandWon() / trade.areaSquareMeters();
    }

    private static long roundPrice(double priceTenThousandWon) {
        return Math.round(priceTenThousandWon / 100.0) * 100L;
    }

    private static boolean sameApartment(String left, String right) {
        if (left == null || right == null) return false;
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static PricePredictionConfidence confidence(int sampleCount, long sameApartmentCount) {
        if (sampleCount >= 15 && sameApartmentCount >= 3) return PricePredictionConfidence.HIGH;
        if (sampleCount >= 7) return PricePredictionConfidence.MEDIUM;
        return PricePredictionConfidence.LOW;
    }

    private record WeightedValue(double value, double weight) {
    }
}
