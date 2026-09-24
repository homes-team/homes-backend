package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.ScoreCalculation;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.ScoreEvidence;
import com.homes.backend.domain.property.repository.StationDistanceProjection;
import com.homes.backend.domain.property.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Produces a deterministic transportation score for any Korean property coordinate using the
 * nationwide subway and bus-stop POI table. The score and its inputs are returned together so downstream
 * reports never need to infer why a number was assigned.
 */
@Component
@RequiredArgsConstructor
public class NationwideTransportScoreProvider {
    static final String POLICY_VERSION = "NATIONWIDE_TRANSPORT_V1";
    private static final String SUBWAY_TYPE = "지하철역";
    private static final String BUS_TYPE = "버스정류장";
    private static final double SUBWAY_RADIUS_METERS = 1_000.0;
    private static final double BUS_RADIUS_METERS = 500.0;
    private static final double SUBWAY_SEARCH_RADIUS_METERS = 20_000.0;
    private static final double BUS_SEARCH_RADIUS_METERS = 5_000.0;
    private static final double METERS_PER_DEGREE = 111_320.0;
    private static final double SUBWAY_WEIGHT = 0.5;
    private static final double BUS_WEIGHT = 0.3;
    private static final double DENSITY_WEIGHT = 0.2;

    private final StationRepository stationRepository;

    public Optional<Result> evaluate(Point coordinate) {
        if (coordinate == null || !Double.isFinite(coordinate.getX()) || !Double.isFinite(coordinate.getY())) {
            return Optional.empty();
        }

        StationDistanceProjection nearestSubway = stationRepository.findNearestByPoiType(
                coordinate, SUBWAY_TYPE, toDegrees(SUBWAY_SEARCH_RADIUS_METERS));
        StationDistanceProjection nearestBus = stationRepository.findNearestByPoiType(
                coordinate, BUS_TYPE, toDegrees(BUS_SEARCH_RADIUS_METERS));
        if (!hasValidDistance(nearestSubway) && !hasValidDistance(nearestBus)) {
            return Optional.empty();
        }

        long subwayCount = stationRepository.countByPoiTypeWithinRadius(
                coordinate, SUBWAY_TYPE, SUBWAY_RADIUS_METERS, toDegrees(SUBWAY_RADIUS_METERS));
        long busCount = stationRepository.countByPoiTypeWithinRadius(
                coordinate, BUS_TYPE, BUS_RADIUS_METERS, toDegrees(BUS_RADIUS_METERS));
        double subwayContribution = hasValidDistance(nearestSubway)
                ? round1(subwayDistanceScore(nearestSubway.getDistance()) * SUBWAY_WEIGHT) : 0.0;
        double busContribution = hasValidDistance(nearestBus)
                ? round1(busDistanceScore(nearestBus.getDistance()) * BUS_WEIGHT) : 0.0;
        double densityScore = densityScore(subwayCount, busCount);
        double densityContribution = round1(densityScore * DENSITY_WEIGHT);
        double score = round1(subwayContribution + busContribution + densityContribution);

        List<ScoreEvidence> evidence = new ArrayList<>();
        if (hasValidDistance(nearestSubway)) {
            evidence.add(new ScoreEvidence(
                        "NEAREST_SUBWAY_DISTANCE",
                        "가장 가까운 지하철역",
                        nearestSubway.getPoiName() + " · " + Math.round(nearestSubway.getDistance()) + "m",
                        null,
                        subwayDistanceCriterion(nearestSubway.getDistance()),
                        subwayContribution,
                        "전국 교통 POI"
                ));
        }
        if (hasValidDistance(nearestBus)) {
            evidence.add(new ScoreEvidence(
                    "NEAREST_BUS_DISTANCE",
                    "가장 가까운 버스정류장",
                    nearestBus.getPoiName() + " · " + Math.round(nearestBus.getDistance()) + "m",
                    null,
                    busDistanceCriterion(nearestBus.getDistance()),
                    busContribution,
                    "전국 교통 POI"
            ));
        }
        evidence.add(new ScoreEvidence(
                        "SUBWAY_COUNT_WITHIN_1KM",
                        "1km 내 지하철역 수",
                        String.valueOf(subwayCount),
                        "곳",
                        "교통수단 밀도에 반영",
                        null,
                        "전국 교통 POI"
                ));
        evidence.add(new ScoreEvidence(
                "BUS_COUNT_WITHIN_500M",
                "500m 내 버스정류장 수",
                String.valueOf(busCount),
                "곳",
                "교통수단 밀도에 반영",
                densityContribution,
                "전국 교통 POI"
        ));
        String description = description(nearestSubway, nearestBus, subwayCount, busCount);
        ScoreCalculation calculation = new ScoreCalculation(
                "지하철 거리 점수×50% + 버스 거리 점수×30% + 주변 교통 밀도 점수×20% = " + score,
                POLICY_VERSION
        );
        return Optional.of(new Result(score, description, List.copyOf(evidence), calculation));
    }

    private boolean hasValidDistance(StationDistanceProjection station) {
        return station != null && station.getDistance() != null && Double.isFinite(station.getDistance());
    }

    private double subwayDistanceScore(double distanceMeters) {
        if (distanceMeters <= 300) return 100.0;
        if (distanceMeters <= 500) return 90.0;
        if (distanceMeters <= 800) return 75.0;
        if (distanceMeters <= 1_200) return 55.0;
        if (distanceMeters <= 2_000) return 30.0;
        return 10.0;
    }

    private double busDistanceScore(double distanceMeters) {
        if (distanceMeters <= 100) return 100.0;
        if (distanceMeters <= 250) return 85.0;
        if (distanceMeters <= 500) return 65.0;
        if (distanceMeters <= 1_000) return 40.0;
        return 15.0;
    }

    private double densityScore(long subwayCount, long busCount) {
        return Math.min(100.0, subwayCount * 20.0 + Math.min(busCount, 10) * 8.0);
    }

    private String subwayDistanceCriterion(double distanceMeters) {
        if (distanceMeters <= 300) return "300m 이하";
        if (distanceMeters <= 500) return "500m 이하";
        if (distanceMeters <= 800) return "800m 이하";
        if (distanceMeters <= 1_200) return "1.2km 이하";
        if (distanceMeters <= 2_000) return "2km 이하";
        return "2km 초과";
    }

    private String busDistanceCriterion(double distanceMeters) {
        if (distanceMeters <= 100) return "100m 이하";
        if (distanceMeters <= 250) return "250m 이하";
        if (distanceMeters <= 500) return "500m 이하";
        if (distanceMeters <= 1_000) return "1km 이하";
        return "1km 초과";
    }

    private String description(
            StationDistanceProjection subway,
            StationDistanceProjection bus,
            long subwayCount,
            long busCount
    ) {
        List<String> facts = new ArrayList<>();
        if (hasValidDistance(subway)) {
            facts.add("가장 가까운 지하철역은 " + subway.getPoiName() + "(약 "
                    + Math.round(subway.getDistance()) + "m)");
        }
        if (hasValidDistance(bus)) {
            facts.add("가장 가까운 버스정류장은 " + bus.getPoiName() + "(약 "
                    + Math.round(bus.getDistance()) + "m)");
        }
        return String.join(", ", facts) + "이며, 1km 내 지하철역 " + subwayCount
                + "곳과 500m 내 버스정류장 " + busCount + "곳을 점수에 반영했습니다.";
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double toDegrees(double meters) {
        return meters / METERS_PER_DEGREE;
    }

    public record Result(
            double score,
            String description,
            List<ScoreEvidence> evidence,
            ScoreCalculation calculation
    ) {}
}
