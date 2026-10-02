package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyOption;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
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
import java.time.Year;
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
    private final KakaoNearbySchoolProvider nearbySchoolProvider;
    private final KakaoNearbyInfrastructureProvider nearbyInfrastructureProvider;
    private final NationwideTransportScoreProvider nationwideTransportScoreProvider;
    private final PropertyBuildingInformationRepository buildingInformationRepository;
    private final PropertyEvaluationScorePolicy evaluationScorePolicy;
    private final AiEvaluationReportService evaluationReportService;

    /**
     * Builds the six-category AI evaluation for a property from stored or bundled data.
     *
     * @param propertyId property identifier
     * @return evaluation scores and generated report
     */
    public AiEvaluationRespDto getAiEvaluation(Long propertyId) {
        Property property = getProperty(propertyId);
        Optional<PropertyAiEvaluation> stored = evaluationRepository.findById(propertyId);
        Optional<DobongAiDataset.Entry> dataset = stored.isEmpty()
                ? dobongAiDataset.findByAddress(property.getAddress())
                : Optional.empty();
        boolean useNearbyProviders = stored.isEmpty() && dataset.isEmpty();
        Optional<KakaoNearbySchoolProvider.Result> nearbySchools = useNearbyProviders
                ? nearbySchoolProvider.evaluate(property.getCoordinate()) : Optional.empty();
        Optional<KakaoNearbyInfrastructureProvider.Result> nearbyInfrastructure = useNearbyProviders
                ? nearbyInfrastructureProvider.evaluate(property.getCoordinate()) : Optional.empty();
        Optional<NationwideTransportScoreProvider.Result> nationwideTransport = dataset.isEmpty()
                ? nationwideTransportScoreProvider.evaluate(property.getCoordinate())
                : Optional.empty();
        Optional<PropertyBuildingInformation> buildingInformation = stored.isEmpty()
                ? buildingInformationRepository.findById(propertyId)
                : Optional.empty();

        PropertyEvaluationScorePolicy.SunlightScoreResult sunlightResult =
                evaluationScorePolicy.evaluateSunlight(
                        property.getDirection(), property.getCurrentFloor(), property.getTotalFloors());

        Double schoolScore = stored.map(PropertyAiEvaluation::getSchoolScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::educationScore)
                        .orElseGet(() -> nearbySchools.map(KakaoNearbySchoolProvider.Result::score).orElse(null)));
        Double transportScore = stored.map(PropertyAiEvaluation::getTransportScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::transportationScore)
                        .orElseGet(() -> nationwideTransport
                                .map(NationwideTransportScoreProvider.Result::score).orElse(null)));
        Double natureScore = stored.map(PropertyAiEvaluation::getNatureScore)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::parkScore).orElse(null));
        Double sunlightScore = stored.map(PropertyAiEvaluation::getSunlightScore)
                .orElseGet(() -> sunlightResult == null ? null : sunlightResult.score());
        Integer buildingYear = buildingInformation.map(PropertyBuildingInformation::getBuildingYear)
                .orElseGet(() -> dataset.map(DobongAiDataset.Entry::buildYear).orElse(null));
        boolean hasElevator = buildingInformation.map(info -> positive(info.getElevatorCount()))
                .orElseGet(() -> property.getOptions().contains(PropertyOption.ELEVATOR));
        boolean hasParking = buildingInformation.map(info -> positive(info.getParkingCount()))
                .orElseGet(() -> property.getOptions().contains(PropertyOption.PARKING));
        String heatingType = buildingInformation.map(PropertyBuildingInformation::getHeatingType).orElse(null);
        PropertyEvaluationScorePolicy.BuildingConditionScoreResult buildingResult =
                evaluationScorePolicy.evaluateBuildingCondition(buildingYear, property.getRemodelingYear(),
                        hasElevator, hasParking, heatingType != null && !heatingType.isBlank());
        Double buildingScore = stored.map(PropertyAiEvaluation::getBuildingConditionScore)
                .orElseGet(() -> buildingResult == null ? null : buildingResult.score());
        Double infrastructureScore = stored.map(PropertyAiEvaluation::getInfrastructureScore)
                .orElseGet(() -> dataset.map(PropertyInsightService::calculateInfrastructureScore)
                        .orElseGet(() -> nearbyInfrastructure
                                .map(KakaoNearbyInfrastructureProvider.Result::score).orElse(null)));

        ScoreSource legacyOrStoredSource = dataset.isPresent()
                ? ScoreSource.EXTERNAL_DATA : ScoreSource.GEOSPATIAL_PIPELINE;
        ScoreSource transportSource = dataset.isPresent()
                ? ScoreSource.EXTERNAL_DATA : ScoreSource.GEOSPATIAL_PIPELINE;
        ScoreSource buildingSource = buildingInformation.isPresent() || dataset.isPresent()
                ? ScoreSource.PROPERTY_RULE : ScoreSource.GEOSPATIAL_PIPELINE;

        List<CategoryScore> categories = List.of(
                category(CategoryKey.SCHOOL, "학군지", schoolScore, legacyOrStoredSource,
                        schoolDescription(dataset, nearbySchools), schoolEvidence(dataset, nearbySchools),
                        schoolCalculation(schoolScore, dataset, stored, nearbySchools)),
                category(CategoryKey.TRANSPORT, "교통", transportScore, transportSource,
                        transportDescription(dataset, nationwideTransport),
                        transportEvidence(dataset, nationwideTransport),
                        transportCalculation(dataset, nationwideTransport, stored, transportScore)),
                category(CategoryKey.NATURE, "자연", natureScore, legacyOrStoredSource,
                        natureDescription(dataset), natureEvidence(dataset),
                        precomputedCalculation(natureScore, dataset, stored)),
                category(CategoryKey.SUNLIGHT, "일조량", sunlightScore, ScoreSource.PROPERTY_RULE,
                        sunlightDescription(property), sunlightEvidence(property, sunlightResult),
                        sunlightCalculation(sunlightResult)),
                category(CategoryKey.BUILDING_CONDITION, "건물 상태", buildingScore, buildingSource,
                        buildingDescription(buildingYear, property.getRemodelingYear(), hasElevator,
                                hasParking, heatingType),
                        buildingEvidence(buildingYear, property.getRemodelingYear(), hasElevator,
                                hasParking, heatingType, buildingResult),
                        buildingCalculation(buildingResult)),
                category(CategoryKey.INFRASTRUCTURE, "인프라", infrastructureScore, legacyOrStoredSource,
                        infrastructureDescription(dataset, nearbyInfrastructure),
                        infrastructureEvidence(dataset, nearbyInfrastructure),
                        infrastructureCalculation(infrastructureScore, dataset, stored, nearbyInfrastructure))
        );

        List<Double> availableScores = categories.stream().map(CategoryScore::rawScore).filter(v -> v != null).toList();
        int evaluatedCount = availableScores.size();
        Double rawOverall = evaluatedCount < 4
                ? null
                : round1(availableScores.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        double completeness = Math.round(evaluatedCount / 6.0 * 1000.0) / 10.0;

        String notice = evaluatedCount < 6 ? "평가 근거가 확보되지 않은 항목은 종합점수에서 제외했습니다." : null;
        Report ruleBasedReport = buildReport(categories, notice);
        LocalDateTime scoreGeneratedAt = stored.map(PropertyAiEvaluation::getGeneratedAt)
                .orElseGet(() -> dataset.isPresent() ? dobongAiDataset.loadedAt() : LocalDateTime.now());
        String scoreVersion = stored.map(PropertyAiEvaluation::getScoreVersion)
                .orElseGet(() -> dataset.isPresent()
                        ? "DOBONG_GEOSPATIAL_V1"
                        : nearbySchools.isPresent() || nationwideTransport.isPresent() || nearbyInfrastructure.isPresent()
                                ? "NATIONWIDE_EXPLAINABLE_V3" : "PROPERTY_RULE_V2");
        AiEvaluationReportService.Resolution reportResolution = evaluationReportService.resolve(
                property, categories, scoreVersion, notice, ruleBasedReport, scoreGeneratedAt);

        return new AiEvaluationRespDto(
                propertyId,
                new OverallScore(rawOverall, toDisplayScore(rawOverall), evaluatedCount, 6, completeness),
                categories,
                reportResolution.report(),
                scoreVersion,
                reportResolution.modelVersion(),
                reportResolution.generatedAt()
        );
    }

    /**
     * Builds a virtual isochrone polygon for a supported travel mode and duration.
     *
     * @param propertyId property identifier
     * @param requestedMode requested travel mode
     * @param travelTimeMinutes requested travel duration in minutes
     * @return isochrone response
     */
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

    /**
     * Loads a property or raises the domain-specific not-found error.
     *
     * @param propertyId property identifier
     * @return persisted property
     */
    private Property getProperty(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
    }

    /**
     * Creates a category response while normalizing its raw and display scores.
     *
     * @param key category identifier
     * @param label display label
     * @param score raw score
     * @param source score source
     * @param description category explanation
     * @return normalized category response
     */
    private CategoryScore category(
            CategoryKey key,
            String label,
            Double score,
            ScoreSource source,
            String description,
            List<ScoreEvidence> evidence,
            ScoreCalculation calculation
    ) {
        Double normalized = score == null ? null : round1(Math.max(0, Math.min(100, score)));
        return new CategoryScore(
                key,
                label,
                normalized,
                toDisplayScore(normalized),
                normalized == null ? ScoreStatus.PENDING_DATA : ScoreStatus.AVAILABLE,
                normalized == null ? ScoreSource.NONE : source,
                description,
                normalized == null ? List.of() : evidence,
                normalized == null ? null : calculation
        );
    }

    /**
     * Converts a 100-point score to the one-decimal, five-point display scale.
     *
     * @param rawScore score on the 100-point scale
     * @return score on the five-point scale, or {@code null}
     */
    static Double toDisplayScore(Double rawScore) {
        return rawScore == null ? null : Math.round(rawScore / 20.0 * 10.0) / 10.0;
    }

    /**
     * Calculates the equally weighted infrastructure score.
     *
     * @param entry source dataset entry
     * @return infrastructure score
     */
    static double calculateInfrastructureScore(DobongAiDataset.Entry entry) {
        return round1((entry.hospitalScore() + entry.martScore() + entry.cultureScore()) / 3.0);
    }

    /**
     * Rounds a number to one decimal place.
     *
     * @param value number to round
     * @return rounded value
     */
    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * Describes nearby education facilities when dataset details are available.
     *
     * @param dataset matching dataset entry
     * @return school-category description
     */
    private String schoolDescription(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<KakaoNearbySchoolProvider.Result> nearbySchools
    ) {
        return nearbySchools.map(KakaoNearbySchoolProvider.Result::description)
                .orElseGet(() -> dataset.map(entry -> "가장 가까운 학교까지 약 " + Math.round(entry.nearestSchoolDistanceMeters())
                + "m이며, 1km 내 초·중·고교가 "
                + (entry.elementaryCountWithin1km() + entry.middleCountWithin1km() + entry.highCountWithin1km())
                + "곳 있습니다.")
                .orElse("학교 접근성과 주변 교육 인프라를 기준으로 산정합니다."));
    }

    /**
     * Describes nearby public transportation when dataset details are available.
     *
     * @param dataset matching dataset entry
     * @return transportation-category description
     */
    private String transportDescription(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<NationwideTransportScoreProvider.Result> nationwideTransport
    ) {
        return dataset.map(entry -> "가장 가까운 지하철역은 " + entry.nearestSubway() + "(약 "
                + Math.round(entry.subwayDistanceMeters()) + "m), 버스정류장은 " + entry.nearestBus()
                + "(약 " + Math.round(entry.busDistanceMeters()) + "m)입니다.")
                .orElseGet(() -> nationwideTransport
                        .map(NationwideTransportScoreProvider.Result::description)
                        .orElse("지하철역 거리와 주변 교통 밀도를 기준으로 산정합니다."));
    }

    /**
     * Describes nearby parks when dataset details are available.
     *
     * @param dataset matching dataset entry
     * @return nature-category description
     */
    private String natureDescription(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.map(entry -> "가장 가까운 공원은 " + entry.nearestPark() + "이며 약 "
                + Math.round(entry.parkDistanceMeters()) + "m 거리입니다.")
                .orElse("공원과 녹지의 거리 및 밀도를 기준으로 산정합니다.");
    }

    /**
     * Describes the direction and floor inputs used for the sunlight score.
     *
     * @param property property whose sunlight inputs were evaluated
     * @return sunlight-category description
     */
    private String sunlightDescription(Property property) {
        if (property.getDirection() == null
                || property.getDirection() == com.homes.backend.domain.property.entity.PropertyDirection.UNKNOWN) {
            return "주실 방향 정보가 입력되면 현재 층수와 함께 평가에 반영됩니다.";
        }
        return property.getDirection().name() + " 방향과 " + property.getCurrentFloor()
                + "/" + property.getTotalFloors() + "층 정보를 기준으로 산정한 점수입니다.";
    }

    /**
     * Describes the construction and optional remodeling years used for the building score.
     *
     * @param buildingYear construction year, or {@code null} when unavailable
     * @param remodelingYear latest remodeling year, or {@code null} when unavailable
     * @return building-condition description
     */
    private String buildingDescription(
            Integer buildingYear,
            Integer remodelingYear,
            boolean hasElevator,
            boolean hasParking,
            String heatingType
    ) {
        if (buildingYear == null) {
            return "준공연도가 수집되면 건물 상태 평가에 반영됩니다.";
        }
        int age = Math.max(0, Year.now().getValue() - buildingYear);
        List<String> facts = new ArrayList<>();
        facts.add(buildingYear + "년 준공, 현재 " + age + "년차");
        if (remodelingYear != null) facts.add(remodelingYear + "년 인테리어");
        facts.add(hasElevator ? "엘리베이터 있음" : "엘리베이터 정보 없음");
        facts.add(hasParking ? "주차 가능" : "주차 정보 없음");
        if (heatingType != null && !heatingType.isBlank()) facts.add(heatingType);
        return String.join(" · ", facts) + "을 반영했습니다.";
    }

    /**
     * Describes the facilities included in the infrastructure score.
     *
     * @param dataset matching dataset entry
     * @return infrastructure-category description
     */
    private String infrastructureDescription(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<KakaoNearbyInfrastructureProvider.Result> nearbyInfrastructure
    ) {
        return nearbyInfrastructure.map(KakaoNearbyInfrastructureProvider.Result::description)
                .orElseGet(() -> dataset.map(entry -> "병원·마트·문화시설 점수를 동일 가중 평균했습니다. 가까운 병원은 "
                + entry.nearestHospital() + "이며 약 " + Math.round(entry.hospitalDistanceMeters()) + "m 거리입니다.")
                .orElse("병원, 마트, 문화시설 등 생활 편의시설 접근성을 기준으로 산정합니다."));
    }

    private List<ScoreEvidence> schoolEvidence(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<KakaoNearbySchoolProvider.Result> nearbySchools
    ) {
        if (nearbySchools.isPresent()) return nearbySchools.get().evidence();
        return dataset.map(entry -> List.of(
                evidence("NEAREST_SCHOOL_DISTANCE", "가장 가까운 학교", entry.nearestSchoolDistanceMeters(),
                        "m", "학교 접근 거리 반영", null, "도봉구 지리 데이터셋"),
                new ScoreEvidence("SCHOOL_COUNT_WITHIN_1KM", "1km 내 초·중·고교 수",
                        String.valueOf(entry.elementaryCountWithin1km() + entry.middleCountWithin1km()
                                + entry.highCountWithin1km()), "곳", "교육시설 밀도 반영", null,
                        "도봉구 지리 데이터셋")
        )).orElse(List.of());
    }

    private List<ScoreEvidence> transportEvidence(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<NationwideTransportScoreProvider.Result> nationwideTransport
    ) {
        return dataset.map(entry -> List.of(
                new ScoreEvidence("NEAREST_SUBWAY_DISTANCE", "가장 가까운 지하철역",
                        entry.nearestSubway() + " · " + Math.round(entry.subwayDistanceMeters()) + "m",
                        null, "지하철 접근 거리 반영", null, "도봉구 지리 데이터셋"),
                new ScoreEvidence("NEAREST_BUS_DISTANCE", "가장 가까운 버스정류장",
                        entry.nearestBus() + " · " + Math.round(entry.busDistanceMeters()) + "m",
                        null, "버스 접근 거리 반영", null, "도봉구 지리 데이터셋")
        )).orElseGet(() -> nationwideTransport
                .map(NationwideTransportScoreProvider.Result::evidence).orElse(List.of()));
    }

    private ScoreCalculation schoolCalculation(
            Double score,
            Optional<DobongAiDataset.Entry> dataset,
            Optional<PropertyAiEvaluation> stored,
            Optional<KakaoNearbySchoolProvider.Result> nearbySchools
    ) {
        if (score == null) return null;
        if (stored.isPresent() || dataset.isPresent()) return precomputedCalculation(score, dataset, stored);
        return nearbySchools.map(KakaoNearbySchoolProvider.Result::calculation).orElse(null);
    }

    private List<ScoreEvidence> natureEvidence(Optional<DobongAiDataset.Entry> dataset) {
        return dataset.map(entry -> List.of(
                evidence("NEAREST_PARK_DISTANCE", "가장 가까운 공원", entry.parkDistanceMeters(),
                        "m", "공원 접근 거리 반영", null, "도봉구 지리 데이터셋")
        )).orElse(List.of());
    }

    private List<ScoreEvidence> sunlightEvidence(
            Property property,
            PropertyEvaluationScorePolicy.SunlightScoreResult result
    ) {
        if (result == null) return List.of();
        return List.of(
                new ScoreEvidence("MAIN_DIRECTION", "주실 방향", property.getDirection().name(), null,
                        "방향별 기준 점수", result.directionScore(), "매물 입력 정보"),
                new ScoreEvidence("RELATIVE_FLOOR", "현재 층/전체 층",
                        property.getCurrentFloor() + "/" + property.getTotalFloors(), "층",
                        "전체 층 대비 현재 층 보정", result.floorAdjustment(), "매물 입력 정보")
        );
    }

    private List<ScoreEvidence> buildingEvidence(
            Integer buildingYear,
            Integer remodelingYear,
            boolean hasElevator,
            boolean hasParking,
            String heatingType,
            PropertyEvaluationScorePolicy.BuildingConditionScoreResult result
    ) {
        if (buildingYear == null || result == null) return List.of();
        List<ScoreEvidence> evidence = new ArrayList<>();
        evidence.add(new ScoreEvidence("BUILDING_AGE", "건물 연식",
                buildingYear + "년 준공 · " + Math.max(0, Year.now().getValue() - buildingYear) + "년차", null,
                "연식 1년당 2점 차감, 최저 20점",
                round1(result.buildingYearScore() * result.buildingYearWeight()), "건축물 정보"));
        if (remodelingYear != null && result.remodelingYearScore() != null) {
            evidence.add(new ScoreEvidence("REMODELING_YEAR", "최근 인테리어",
                    String.valueOf(remodelingYear), "년", "리모델링 연도 점수 35% 반영",
                    round1(result.remodelingYearScore() * result.remodelingYearWeight()), "매물 입력 정보"));
        }
        evidence.add(new ScoreEvidence("ELEVATOR", "엘리베이터", hasElevator ? "있음" : "정보 없음",
                null, "건물 편의시설", null, "건축물·매물 정보"));
        evidence.add(new ScoreEvidence("PARKING", "주차", hasParking ? "가능" : "정보 없음",
                null, "건물 편의시설", null, "건축물·매물 정보"));
        evidence.add(new ScoreEvidence("HEATING", "난방",
                heatingType == null || heatingType.isBlank() ? "정보 없음" : heatingType,
                null, "건물 편의시설", null, "건축물 정보"));
        return List.copyOf(evidence);
    }

    private List<ScoreEvidence> infrastructureEvidence(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<KakaoNearbyInfrastructureProvider.Result> nearbyInfrastructure
    ) {
        if (nearbyInfrastructure.isPresent()) return nearbyInfrastructure.get().evidence();
        return dataset.map(entry -> List.of(
                evidence("NEAREST_HOSPITAL_DISTANCE", "가장 가까운 병원", entry.hospitalDistanceMeters(),
                        "m", "병원 접근 거리 반영", null, "도봉구 지리 데이터셋"),
                new ScoreEvidence("HOSPITAL_SCORE", "병원 접근성 점수",
                        String.valueOf(entry.hospitalScore()), "점", "동일 가중 평균",
                        round1(entry.hospitalScore() / 3.0), "도봉구 지리 데이터셋"),
                new ScoreEvidence("MART_SCORE", "마트 접근성 점수",
                        String.valueOf(entry.martScore()), "점", "동일 가중 평균",
                        round1(entry.martScore() / 3.0), "도봉구 지리 데이터셋"),
                new ScoreEvidence("CULTURE_SCORE", "문화시설 접근성 점수",
                        String.valueOf(entry.cultureScore()), "점", "동일 가중 평균",
                        round1(entry.cultureScore() / 3.0), "도봉구 지리 데이터셋")
        )).orElse(List.of());
    }

    private ScoreCalculation infrastructureCalculation(
            Double score,
            Optional<DobongAiDataset.Entry> dataset,
            Optional<PropertyAiEvaluation> stored,
            Optional<KakaoNearbyInfrastructureProvider.Result> nearbyInfrastructure
    ) {
        if (score == null) return null;
        if (stored.isPresent() || dataset.isPresent()) return precomputedCalculation(score, dataset, stored);
        return nearbyInfrastructure.map(KakaoNearbyInfrastructureProvider.Result::calculation).orElse(null);
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private ScoreEvidence evidence(
            String code,
            String label,
            double value,
            String unit,
            String criterion,
            Double contribution,
            String source
    ) {
        return new ScoreEvidence(code, label, String.valueOf(Math.round(value)), unit,
                criterion, contribution, source);
    }

    private ScoreCalculation precomputedCalculation(
            Double score,
            Optional<DobongAiDataset.Entry> dataset,
            Optional<PropertyAiEvaluation> stored
    ) {
        if (score == null) return null;
        String policyVersion = dataset.isPresent() ? "DOBONG_GEOSPATIAL_V1"
                : stored.map(PropertyAiEvaluation::getScoreVersion).orElse("PRECOMPUTED_SCORE");
        String formula = dataset.isPresent() ? "사전 산출 지리 점수 = " : "저장된 평가 점수 = ";
        return new ScoreCalculation(formula + round1(score), policyVersion);
    }

    private ScoreCalculation transportCalculation(
            Optional<DobongAiDataset.Entry> dataset,
            Optional<NationwideTransportScoreProvider.Result> nationwideTransport,
            Optional<PropertyAiEvaluation> stored,
            Double score
    ) {
        if (score == null) return null;
        if (dataset.isPresent()) return precomputedCalculation(score, dataset, stored);
        return nationwideTransport.map(NationwideTransportScoreProvider.Result::calculation)
                .orElse(new ScoreCalculation("저장된 교통 점수 = " + round1(score),
                        stored.map(PropertyAiEvaluation::getScoreVersion).orElse("STORED_SCORE")));
    }

    private ScoreCalculation sunlightCalculation(PropertyEvaluationScorePolicy.SunlightScoreResult result) {
        if (result == null) return null;
        return new ScoreCalculation(
                "방향 기준 " + result.directionScore() + " + 층수 보정 " + result.floorAdjustment()
                        + " = " + result.score(),
                PropertyEvaluationScorePolicy.POLICY_VERSION
        );
    }

    private ScoreCalculation buildingCalculation(
            PropertyEvaluationScorePolicy.BuildingConditionScoreResult result
    ) {
        if (result == null) return null;
        String formula = result.remodelingYearScore() == null
                ? "준공연도 점수 + 편의시설 보정 = " + result.score()
                : "준공연도 점수×65% + 리모델링 연도 점수×35% + 편의시설 보정 "
                        + result.facilityAdjustment() + " = " + result.score();
        return new ScoreCalculation(formula, PropertyEvaluationScorePolicy.POLICY_VERSION);
    }

    /**
     * Generates a rule-based summary with up to three strengths and weaknesses.
     *
     * @param categories evaluated categories
     * @param notice incomplete-data notice
     * @return structured evaluation report
     */
    private Report buildReport(List<CategoryScore> categories, String notice) {
        List<CategoryScore> available = categories.stream().filter(c -> c.rawScore() != null).toList();
        if (available.isEmpty()) {
            return new Report("평가 데이터가 준비 중입니다.", List.of(), List.of(), notice);
        }
        List<String> strengths = available.stream().filter(c -> c.rawScore() >= 80)
                .map(c -> c.label() + " " + c.displayScore() + "/5.0: " + c.description()).limit(3).toList();
        List<String> weaknesses = available.stream().filter(c -> c.rawScore() < 60)
                .map(c -> c.label() + " " + c.displayScore() + "/5.0: " + c.description()).limit(3).toList();
        return new Report("확인 가능한 " + available.size() + "개 항목의 점수와 산정 근거를 바탕으로 평가했습니다.",
                strengths, weaknesses, notice);
    }

    /**
     * Creates a deterministic, closed radial polygon around a coordinate.
     *
     * @param latitude center latitude
     * @param longitude center longitude
     * @param radiusMeters nominal polygon radius in meters
     * @param seed property-specific shape seed
     * @return closed longitude-latitude coordinate ring
     */
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
