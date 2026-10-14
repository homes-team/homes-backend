package com.homes.backend.domain.property.price.model;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Component
public class PricePredictionPolicy {
    public static final String METHOD = "COMPARABLE_WEIGHTED_MEDIAN_V2";
    private static final int MINIMUM_SAMPLES = 3;
    private static final int MAXIMUM_SAMPLES = 50;
    private static final int REPRESENTATIVE_SAMPLE_COUNT = 5;

    public PricePrediction predict(PricePredictionTarget target, List<ApartmentTrade> source, LocalDate asOf) {
        if (target.areaSquareMeters() <= 0) {
            return PricePrediction.unavailable(PricePredictionStatus.INSUFFICIENT_DATA,
                    "매물 전용면적이 없어 가격을 예측할 수 없습니다.");
        }
        List<ApartmentTrade> candidates = source.stream()
                .filter(trade -> trade.areaSquareMeters() > 0 && trade.priceTenThousandWon() > 0)
                .filter(trade -> !trade.contractDate().isAfter(asOf))
                .filter(trade -> !trade.contractDate().isBefore(asOf.minusMonths(24)))
                .toList();

        Selection selection = selectBestAvailable(candidates, target, asOf);
        if (selection.trades().size() < MINIMUM_SAMPLES) {
            return PricePrediction.unavailable(PricePredictionStatus.INSUFFICIENT_DATA,
                    "가격을 예측하기 위한 유사 거래가 부족합니다.");
        }
        List<ApartmentTrade> selected = selection.trades();
        List<WeightedValue> values = selected.stream()
                .map(trade -> new WeightedValue(pricePerSquareMeter(trade), weight(trade, target, asOf)))
                .sorted(Comparator.comparingDouble(WeightedValue::value))
                .toList();
        double trendFactor = trendFactor(selected, asOf);
        long predicted = roundPrice(weightedPercentile(values, 0.5) * target.areaSquareMeters() * trendFactor);
        long q1 = roundPrice(weightedPercentile(values, 0.25) * target.areaSquareMeters() * trendFactor);
        long q3 = roundPrice(weightedPercentile(values, 0.75) * target.areaSquareMeters() * trendFactor);
        long minimum = Math.min(q1, roundPrice(predicted * 0.95));
        long maximum = Math.max(q3, roundPrice(predicted * 1.05));

        YearMonth from = selected.stream().map(ApartmentTrade::contractDate).min(LocalDate::compareTo).map(YearMonth::from).orElse(null);
        YearMonth to = selected.stream().map(ApartmentTrade::contractDate).max(LocalDate::compareTo).map(YearMonth::from).orElse(null);
        PricePredictionConfidence confidence = confidence(selection.scope(), selected.size());
        List<ComparableTradeSummary> representatives = selected.stream().limit(REPRESENTATIVE_SAMPLE_COUNT)
                .map(ComparableTradeSummary::from).toList();
        String description = "최근 %s 유사 거래 %d건을 기준으로 계산했습니다."
                .formatted(selection.scope().description(), selected.size());
        return new PricePrediction(PricePredictionStatus.AVAILABLE, predicted, minimum, maximum, confidence,
                selection.scope(), selected.size(), from, to, representatives, METHOD, description);
    }

    private Selection selectBestAvailable(List<ApartmentTrade> source, PricePredictionTarget target, LocalDate asOf) {
        List<SelectionRule> rules = List.of(
                new SelectionRule(ComparisonScope.SAME_COMPLEX, trade -> sameComplex(trade, target), 0.15, 5),
                new SelectionRule(ComparisonScope.SAME_COMPLEX, trade -> sameComplex(trade, target), 0.25, 10),
                new SelectionRule(ComparisonScope.SAME_LEGAL_DONG, trade -> sameLegalDong(trade, target), 0.15, 5),
                new SelectionRule(ComparisonScope.SAME_LEGAL_DONG, trade -> sameLegalDong(trade, target), 0.25, 10),
                new SelectionRule(ComparisonScope.SAME_DISTRICT, trade -> true, 0.15, 5),
                new SelectionRule(ComparisonScope.SAME_DISTRICT, trade -> true, 0.25, 10)
        );
        for (SelectionRule rule : rules) {
            List<ApartmentTrade> selected = source.stream()
                    .filter(rule.scopeFilter())
                    .filter(trade -> areaDifference(trade, target) <= rule.areaTolerance())
                    .filter(trade -> buildingYearDifference(trade, target) <= rule.buildingYearTolerance())
                    .sorted(Comparator.comparingDouble(trade -> similarityDistance(trade, target, asOf)))
                    .limit(MAXIMUM_SAMPLES)
                    .toList();
            selected = removeOutliers(selected);
            if (selected.size() >= MINIMUM_SAMPLES) return new Selection(selected, rule.scope());
        }
        return new Selection(List.of(), ComparisonScope.UNAVAILABLE);
    }

    private List<ApartmentTrade> removeOutliers(List<ApartmentTrade> trades) {
        if (trades.size() < 5) return trades;
        List<Double> prices = trades.stream().map(PricePredictionPolicy::pricePerSquareMeter).sorted().toList();
        double median = percentile(prices, 0.5);
        return trades.stream().filter(trade -> {
            double value = pricePerSquareMeter(trade);
            return value >= median * 0.5 && value <= median * 1.5;
        }).toList();
    }

    private double weight(ApartmentTrade trade, PricePredictionTarget target, LocalDate asOf) {
        long monthsOld = Math.max(0, ChronoUnit.MONTHS.between(YearMonth.from(trade.contractDate()), YearMonth.from(asOf)));
        double recency = 1.0 / (1.0 + monthsOld / 6.0);
        double area = 1.0 / (1.0 + areaDifference(trade, target) * 4.0);
        double floor = 1.0 / (1.0 + Math.abs(trade.floor() - target.floor()) / 10.0);
        double year = 1.0 / (1.0 + buildingYearDifference(trade, target) / 10.0);
        double complex = sameComplex(trade, target) ? 2.0 : 1.0;
        return recency * area * floor * year * complex;
    }

    private double similarityDistance(ApartmentTrade trade, PricePredictionTarget target, LocalDate asOf) {
        long monthsOld = Math.max(0, ChronoUnit.MONTHS.between(YearMonth.from(trade.contractDate()), YearMonth.from(asOf)));
        return areaDifference(trade, target) * 4.0
                + Math.abs(trade.floor() - target.floor()) / 20.0
                + buildingYearDifference(trade, target) / 20.0
                + monthsOld / 24.0;
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
        double threshold = values.stream().mapToDouble(WeightedValue::weight).sum() * percentile;
        double cumulative = 0;
        for (WeightedValue value : values) {
            cumulative += value.weight();
            if (cumulative >= threshold) return value.value();
        }
        return values.get(values.size() - 1).value();
    }

    private static double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) return 0;
        return sorted.get((int) Math.floor((sorted.size() - 1) * percentile));
    }

    private static double pricePerSquareMeter(ApartmentTrade trade) {
        return trade.priceTenThousandWon() / trade.areaSquareMeters();
    }

    private static long roundPrice(double priceTenThousandWon) {
        return Math.round(priceTenThousandWon / 100.0) * 100L;
    }

    private static double areaDifference(ApartmentTrade trade, PricePredictionTarget target) {
        return Math.abs(trade.areaSquareMeters() - target.areaSquareMeters()) / target.areaSquareMeters();
    }

    private static int buildingYearDifference(ApartmentTrade trade, PricePredictionTarget target) {
        return target.buildYear() == null || trade.buildYear() == null
                ? 0 : Math.abs(trade.buildYear() - target.buildYear());
    }

    private static boolean sameComplex(ApartmentTrade trade, PricePredictionTarget target) {
        if (sameText(trade.apartmentName(), target.apartmentName())) return true;
        return sameLegalDong(trade, target) && sameText(trade.lotNumber(), target.lotNumber());
    }

    private static boolean sameLegalDong(ApartmentTrade trade, PricePredictionTarget target) {
        return sameText(trade.legalDongName(), target.legalDongName());
    }

    private static boolean sameText(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) return false;
        return normalize(left).equals(normalize(right));
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "").replaceFirst("^0+(?!$)", "").toLowerCase(Locale.ROOT);
    }

    private static PricePredictionConfidence confidence(ComparisonScope scope, int sampleCount) {
        if (scope == ComparisonScope.SAME_COMPLEX && sampleCount >= 5) return PricePredictionConfidence.HIGH;
        if ((scope == ComparisonScope.SAME_COMPLEX || scope == ComparisonScope.SAME_LEGAL_DONG) && sampleCount >= 5) {
            return PricePredictionConfidence.MEDIUM;
        }
        return PricePredictionConfidence.LOW;
    }

    private record Selection(List<ApartmentTrade> trades, ComparisonScope scope) {
    }

    private record SelectionRule(ComparisonScope scope, Predicate<ApartmentTrade> scopeFilter,
                                 double areaTolerance, int buildingYearTolerance) {
    }

    private record WeightedValue(double value, double weight) {
    }
}
