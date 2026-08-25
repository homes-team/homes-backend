package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.insight.data.DobongAiDataset;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto;
import com.homes.backend.domain.property.insight.dto.IsochroneRespDto;
import com.homes.backend.domain.property.insight.entity.PropertyAiEvaluation;
import com.homes.backend.domain.property.insight.repository.PropertyAiEvaluationRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyInsightService {
    private static final List<Integer> SUPPORTED_TIMES = List.of(5, 10, 15, 20, 30);
    private static final int POLYGON_VERTEX_COUNT = 36;

    private final PropertyRepository propertyRepository;
    private final PropertyAiEvaluationRepository evaluationRepository;
    private final DobongAiDataset dobongAiDataset;

    public AiEvaluationRespDto getAiEvaluation(Long propertyId) {
        Property property = getProperty(propertyId);
        Optional<PropertyAiEvaluation> stored = evaluationRepository.findById(propertyId);
        Optional<DobongAiDataset.Entry> dataset = stored.isEmpty()
                ? dobongAiDataset.findByAddress(property.getAddress())
                : Optional.empty();

        Double schoolScore = stored.map(PropertyAiEvaluation::getSchoolScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::educationScore).orElse(null));
        Double transportScore = stored.map(PropertyAiEvaluation::getTransportScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::transportationScore).orElse(null));
        Double natureScore = stored.map(PropertyAiEvaluation::getNatureScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::parkScore).orElse(null));
        Double sunlightScore = stored.map(PropertyAiEvaluation::getSunlightScore).orElse(null);
        Double buildingScore = stored.map(PropertyAiEvaluation::getBuildingConditionScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::buildYear)
                        .map(PropertyInsightService::calculateBuildingConditionScore).orElse(null));
        Double infrastructureScore = stored.map(PropertyAiEvaluation::getInfrastructureScore)
                .orElseGet(() -> dataset.map(PropertyInsightService::calculateInfrastructureScore).orElse(null));

        ScoreSource geospatialSource = dataset.isPresent() ? ScoreSource.EXTERNAL_DATA : ScoreSource.GEOSPATIAL_PIPELINE;
        ScoreSource buildingSource = dataset.isPresent() ? ScoreSource.PROPERTY_RULE : ScoreSource.GEOSPATIAL_PIPELINE;

        List<CategoryScore> categories = List.of(
                category(CategoryKey.SCHOOL, "학군지", schoolScore, geospatialSource, schoolDescription(dataset)),
                category(CategoryKey.TRANSPORT, "교통", transportScore, geospatialSource, transportDescription(dataset)),
                category(CategoryKey.NATURE, "자연", natureScore, geospatialSource, natureDescription(dataset)),
                category(CategoryKey.SUNLIGHT, "일조량", sunlightScore, ScoreSource.PROPERTY_RULE, "매물 방향 정보가 수집되면 층수와 함께 평가에 반영됩니다."),
                category(CategoryKey.BUILDING_CONDITION, "건물 상태", buildingScore, buildingSource, buildingDescription(dataset)),
                category(CategoryKey.INFRASTRUCTURE, "인프라", infrastructureScore, geospatialSource, infrastructureDescription(dataset))
        );

        List<Double> availableScores = categories.stream().map(CategoryScore::rawScore).filter(v -> v != null).toList();
        int evaluatedCount = availableScores.size();
        Double rawOverall = availableScores.isEmpty()
                ? null
                : round1(availableScores.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        double completeness = Math.round(evaluatedCount / 6.0 * 1000.0) / 10.0;

        String notice = evaluatedCount < 6 ? "일부 평가 항목은 데이터 수집 후 반영될 예정입니다." : null;
        Report report = buildReport(categories, notice);
        LocalDateTime generatedAt = stored.map(PropertyAiEvaluation::getGeneratedAt)
                .orElseGet(() -> dataset.isPresent() ? dobongAiDataset.loadedAt() : LocalDateTime.now());

        return new AiEvaluationRespDto(
                propertyId,
                new OverallScore(rawOverall, toDisplayScore(rawOverall), evaluatedCount, 6, completeness),
                categories,
                report,
                stored.map(PropertyAiEvaluation::getScoreVersion)
                        .orElseGet(() -> dataset.isPresent() ? "DOBONG_GEOSPATIAL_V1" : "PENDING"),
                "RULE_BASED_REPORT_V1",
                generatedAt
        );
    }

    public IsochroneRespDto getIsochrone(Long propertyId, String requestedMode, int travelTimeMinutes) {
        Property property = getProperty(propertyId);
        String mode = requestedMode == null ? "" : requestedMode.toLowerCase(Locale.ROOT);
        if (!(mode.equals("walk") || mode.equals("drive")) || !SUPPORTED_TIMES.contains(travelTimeMinutes)) {
            throw new CustomException(PropertyErrorCode.INVALID_ISOCHRONE_PARAMETER);
        }

        Point coordinate = property.getCoordinate();
        if (coordinate == null || !Double.isFinite(coordinate.getX()) || !Double.isFinite(coordinate.getY())) {
            throw new CustomException(PropertyErrorCode.ISOCHRONE_COORDINATE_UNAVAILABLE);
        }

        int metersPerMinute = mode.equals("walk") ? 80 : 350;
        int distanceMeters = metersPerMinute * travelTimeMinutes;
        double latitude = coordinate.getY();
        double longitude = coordinate.getX();
        List<List<Double>> ring = createVirtualRing(latitude, longitude, distanceMeters, propertyId);
        double area = Math.PI * distanceMeters * distanceMeters;

        return new IsochroneRespDto(
                propertyId,
                mode,
                travelTimeMinutes,
                new IsochroneRespDto.Center(latitude, longitude),
                new IsochroneRespDto.Geometry("Polygon", List.of(ring)),
                new IsochroneRespDto.Metadata(
                        distanceMeters,
                        Math.round(area * 10.0) / 10.0,
                        "VIRTUAL_RADIAL_POLYGON",
                        "PROPERTY_COORDINATE",
                        false,
                        LocalDateTime.now()
                )
        );
    }

    private Property getProperty(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
    }

    private CategoryScore category(CategoryKey key, String label, Double score, ScoreSource source, String description) {
        Double normalized = score == null ? null : round1(Math.max(0, Math.min(100, score)));
        return new CategoryScore(
                key,
                label,
                normalized,
                toDisplayScore(normalized),
                normalized == null ? ScoreStatus.PENDING_DATA : ScoreStatus.AVAILABLE,
                normalized == null ? ScoreSource.NONE : source,
                description
        );
    }

    static Double toDisplayScore(Double rawScore) {
        return rawScore == null ? null : Math.round(rawScore / 20.0 * 10.0) / 10.0;
    }

    static double calculateInfrastructureScore(DobongAiDataset.Entry entry) {
        return round1((entry.hospitalScore() + entry.martScore() + entry.cultureScore()) / 3.0);
    }

    static double calculateBuildingConditionScore(int buildYear) {
        int age = Math.max(0, LocalDateTime.now().getYear() - buildYear);
        return Math.max(20.0, Math.min(100.0, 100.0 - age * 2.0));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String schoolDescription(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.map(entry -> "가장 가까운 학교까지 약 " + Math.round(entry.nearestSchoolDistanceMeters())
                + "m이며, 1km 내 초·중·고교가 "
                + (entry.elementaryCountWithin1km() + entry.middleCountWithin1km() + entry.highCountWithin1km())
                + "곳 있습니다.")
                .orElse("학교 접근성과 주변 교육 인프라를 기준으로 산정합니다.");
    }

    private String transportDescription(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.map(entry -> "가장 가까운 지하철역은 " + entry.nearestSubway() + "(약 "
                + Math.round(entry.subwayDistanceMeters()) + "m), 버스정류장은 " + entry.nearestBus()
                + "(약 " + Math.round(entry.busDistanceMeters()) + "m)입니다.")
                .orElse("지하철역 거리와 주변 버스 교통 밀도를 기준으로 산정합니다.");
    }

    private String natureDescription(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.map(entry -> "가장 가까운 공원은 " + entry.nearestPark() + "이며 약 "
                + Math.round(entry.parkDistanceMeters()) + "m 거리입니다.")
                .orElse("공원과 녹지의 거리 및 밀도를 기준으로 산정합니다.");
    }

    private String buildingDescription(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.flatMap(entry -> Optional.ofNullable(entry.buildYear()))
                .map(year -> year + "년 준공 정보를 기준으로 산정한 점수입니다.")
                .orElse("준공연도와 건물 시설 정보가 수집되면 평가에 반영됩니다.");
    }

    private String infrastructureDescription(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.map(entry -> "병원·마트·문화시설 점수를 동일 가중 평균했습니다. 가까운 병원은 "
                + entry.nearestHospital() + "이며 약 " + Math.round(entry.hospitalDistanceMeters()) + "m 거리입니다.")
                .orElse("병원, 마트, 문화시설 등 생활 편의시설 접근성을 기준으로 산정합니다.");
    }

    private Report buildReport(List<CategoryScore> categories, String notice) {
        List<CategoryScore> available = categories.stream().filter(c -> c.rawScore() != null).toList();
        if (available.isEmpty()) {
            return new Report("평가 데이터가 준비 중입니다.", List.of(), List.of(), notice);
        }
        List<String> strengths = available.stream().filter(c -> c.rawScore() >= 80)
                .map(c -> c.label() + " 접근성과 여건이 우수합니다.").limit(3).toList();
        List<String> weaknesses = available.stream().filter(c -> c.rawScore() < 60)
                .map(c -> c.label() + " 점수가 상대적으로 낮습니다.").limit(3).toList();
        return new Report("수집된 입지 데이터를 기준으로 매물의 생활 여건을 평가했습니다.", strengths, weaknesses, notice);
    }

    private List<List<Double>> createVirtualRing(double latitude, double longitude, int radiusMeters, long seed) {
        double latitudeDegreePerMeter = 1.0 / 111_320.0;
        double longitudeDegreePerMeter = 1.0 / (111_320.0 * Math.cos(Math.toRadians(latitude)));
        List<List<Double>> ring = new ArrayList<>();
        for (int i = 0; i < POLYGON_VERTEX_COUNT; i++) {
            double angle = Math.PI * 2 * i / POLYGON_VERTEX_COUNT;
            double variation = 0.82 + 0.12 * Math.sin(angle * 3 + seed % 7) + 0.06 * Math.cos(angle * 5);
            double radius = radiusMeters * variation;
            double lat = latitude + Math.sin(angle) * radius * latitudeDegreePerMeter;
            double lng = longitude + Math.cos(angle) * radius * longitudeDegreePerMeter;
            ring.add(List.of(lng, lat));
        }
        ring.add(ring.getFirst());
        return ring;
    }
}
