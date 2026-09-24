package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyDirection;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.insight.data.DobongAiDataset;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto;
import com.homes.backend.domain.property.insight.dto.IsochroneRespDto;
import com.homes.backend.domain.property.insight.repository.PropertyAiEvaluationRepository;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyInsightServiceTest {
    @Mock PropertyRepository propertyRepository;
    @Mock PropertyAiEvaluationRepository evaluationRepository;
    @Mock DobongAiDataset dobongAiDataset;
    @Mock KakaoNearbySchoolProvider nearbySchoolProvider;
    @Mock KakaoNearbyInfrastructureProvider nearbyInfrastructureProvider;
    @Mock NationwideTransportScoreProvider nationwideTransportScoreProvider;
    @Mock PropertyBuildingInformationRepository buildingInformationRepository;
    @Mock AiEvaluationReportService evaluationReportService;

    private PropertyInsightService service;
    private Property property;

    /**
     * Creates the service under test and a property with a valid coordinate.
     */
    @BeforeEach
    void setUp() {
        service = new PropertyInsightService(propertyRepository, evaluationRepository, dobongAiDataset,
                nearbySchoolProvider, nearbyInfrastructureProvider, nationwideTransportScoreProvider,
                buildingInformationRepository,
                new PropertyEvaluationScorePolicy(), evaluationReportService);
        Point point = new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688));
        point.setSRID(4326);
        property = Property.builder()
                .title("테스트 매물")
                .description("설명")
                .address("서울시 도봉구")
                .detailAddress("101호")
                .deposit(1000L)
                .monthlyRent(50L)
                .maintenanceFee(10L)
                .totalFloors(5)
                .currentFloor(3)
                .direction(PropertyDirection.SOUTH)
                .remodelingYear(2022)
                .area(23.0)
                .aiScore(85)
                .coordinate(point)
                .desiredBrokerageFee(0.4)
                .status(PropertyStatus.AVAILABLE)
                .build();
    }

    /**
     * Verifies conversion from a 100-point score to the five-point display scale.
     */
    @Test
    void convertsHundredPointScoreToFivePointScore() {
        assertThat(PropertyInsightService.toDisplayScore(86.0)).isEqualTo(4.3);
        assertThat(PropertyInsightService.toDisplayScore(100.0)).isEqualTo(5.0);
        assertThat(PropertyInsightService.toDisplayScore(null)).isNull();
    }

    /**
     * Verifies that property fields can provide a rule score before geospatial data is loaded.
     */
    @Test
    void calculatesPropertyRuleScoreWhenGeospatialScoresAreNotLoaded() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(evaluationRepository.findById(1L)).thenReturn(Optional.empty());
        when(dobongAiDataset.findByAddress(property.getAddress())).thenReturn(Optional.empty());
        when(nearbySchoolProvider.evaluate(property.getCoordinate())).thenReturn(Optional.empty());
        when(nationwideTransportScoreProvider.evaluate(property.getCoordinate())).thenReturn(Optional.empty());
        when(buildingInformationRepository.findById(1L)).thenReturn(Optional.empty());
        when(evaluationReportService.resolve(
                eq(property), anyList(), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> new AiEvaluationReportService.Resolution(
                        invocation.getArgument(4),
                        AiEvaluationReportService.RULE_BASED_MODEL_VERSION,
                        invocation.getArgument(5)
                ));

        AiEvaluationRespDto response = service.getAiEvaluation(1L);

        assertThat(response.categories()).hasSize(6);
        assertThat(response.categories().stream()
                .filter(category -> category.key() == AiEvaluationRespDto.CategoryKey.SUNLIGHT)
                .findFirst().orElseThrow())
                .satisfies(category -> {
                    assertThat(category.status()).isEqualTo(AiEvaluationRespDto.ScoreStatus.AVAILABLE);
                    assertThat(category.evidence()).hasSize(2);
                    assertThat(category.calculation().policyVersion()).isEqualTo("PROPERTY_RULE_V2");
                });
        assertThat(response.overall().rawScore()).isNull();
        assertThat(response.overall().displayScore()).isNull();
        assertThat(response.overall().completeness()).isEqualTo(16.7);
    }

    /**
     * Verifies that a property outside Dobong-gu receives a coordinate-based transport score and evidence.
     */
    @Test
    void evaluatesTransportationNationwideWithoutDobongAddressMatch() {
        setAddress(property, "부산광역시 부산진구 부전동 123");
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(evaluationRepository.findById(1L)).thenReturn(Optional.empty());
        when(dobongAiDataset.findByAddress(property.getAddress())).thenReturn(Optional.empty());
        when(nearbySchoolProvider.evaluate(property.getCoordinate())).thenReturn(Optional.empty());
        when(buildingInformationRepository.findById(1L)).thenReturn(Optional.empty());
        var transportResult = new NationwideTransportScoreProvider.Result(
                84.0,
                "가장 가까운 지하철역은 서면역(약 420m)이며, 1km 내 지하철역이 2곳 있습니다.",
                List.of(new AiEvaluationRespDto.ScoreEvidence(
                        "NEAREST_SUBWAY_DISTANCE", "가장 가까운 지하철역", "420", "m",
                        "500m 이하", 63.0, "전국 교통 POI")),
                new AiEvaluationRespDto.ScoreCalculation(
                        "최근접 지하철 거리 점수×70% + 1km 내 역 수 점수×30% = 84.0",
                        "NATIONWIDE_TRANSPORT_V1")
        );
        when(nationwideTransportScoreProvider.evaluate(property.getCoordinate()))
                .thenReturn(Optional.of(transportResult));
        when(evaluationReportService.resolve(eq(property), anyList(), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> new AiEvaluationReportService.Resolution(
                        invocation.getArgument(4), AiEvaluationReportService.RULE_BASED_MODEL_VERSION,
                        LocalDateTime.now()));

        AiEvaluationRespDto response = service.getAiEvaluation(1L);

        AiEvaluationRespDto.CategoryScore transport = response.categories().stream()
                .filter(category -> category.key() == AiEvaluationRespDto.CategoryKey.TRANSPORT)
                .findFirst().orElseThrow();
        assertThat(transport.rawScore()).isEqualTo(84.0);
        assertThat(transport.source()).isEqualTo(AiEvaluationRespDto.ScoreSource.GEOSPATIAL_PIPELINE);
        assertThat(transport.evidence()).extracting(AiEvaluationRespDto.ScoreEvidence::source)
                .containsExactly("전국 교통 POI");
        assertThat(response.scoreVersion()).isEqualTo("NATIONWIDE_EXPLAINABLE_V3");
    }

    @Test
    void exposesNearestElementaryMiddleAndHighSchoolNames() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(evaluationRepository.findById(1L)).thenReturn(Optional.empty());
        when(dobongAiDataset.findByAddress(property.getAddress())).thenReturn(Optional.empty());
        when(buildingInformationRepository.findById(1L)).thenReturn(Optional.empty());
        when(nationwideTransportScoreProvider.evaluate(property.getCoordinate())).thenReturn(Optional.empty());
        var schoolResult = new KakaoNearbySchoolProvider.Result(
                90.0,
                "가장 가까운 학교는 초 도봉초등학교(약 300m), 중 도봉중학교(약 500m), 고 도봉고등학교(약 700m)입니다.",
                List.of(
                        new AiEvaluationRespDto.ScoreEvidence("NEAREST_ELEMENTARY_SCHOOL",
                                "가장 가까운 초등학교", "도봉초등학교 · 300m", null,
                                "500m 이하", 33.3, "카카오 로컬 학교 검색"),
                        new AiEvaluationRespDto.ScoreEvidence("NEAREST_MIDDLE_SCHOOL",
                                "가장 가까운 중학교", "도봉중학교 · 500m", null,
                                "500m 이하", 33.3, "카카오 로컬 학교 검색"),
                        new AiEvaluationRespDto.ScoreEvidence("NEAREST_HIGH_SCHOOL",
                                "가장 가까운 고등학교", "도봉고등학교 · 700m", null,
                                "1km 이하", 26.7, "카카오 로컬 학교 검색")
                ),
                new AiEvaluationRespDto.ScoreCalculation(
                        "초·중·고 최근접 학교 거리 점수 평균 = 90.0", "NATIONWIDE_SCHOOL_V1")
        );
        when(nearbySchoolProvider.evaluate(property.getCoordinate())).thenReturn(Optional.of(schoolResult));
        when(evaluationReportService.resolve(eq(property), anyList(), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> new AiEvaluationReportService.Resolution(
                        invocation.getArgument(4), AiEvaluationReportService.RULE_BASED_MODEL_VERSION,
                        LocalDateTime.now()));

        AiEvaluationRespDto response = service.getAiEvaluation(1L);

        AiEvaluationRespDto.CategoryScore school = response.categories().stream()
                .filter(category -> category.key() == AiEvaluationRespDto.CategoryKey.SCHOOL)
                .findFirst().orElseThrow();
        assertThat(school.rawScore()).isEqualTo(90.0);
        assertThat(school.evidence()).extracting(AiEvaluationRespDto.ScoreEvidence::value)
                .containsExactly("도봉초등학교 · 300m", "도봉중학교 · 500m", "도봉고등학교 · 700m");
        assertThat(school.description()).contains("도봉초등학교", "도봉중학교", "도봉고등학교");
    }

    private void setAddress(Property target, String address) {
        try {
            var field = Property.class.getDeclaredField("address");
            field.setAccessible(true);
            field.set(target, address);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Verifies that a supported request returns a closed GeoJSON polygon.
     */
    @Test
    void returnsClosedGeoJsonPolygonForSupportedIsochroneRequest() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        IsochroneRespDto response = service.getIsochrone(1L, "walk", 10);
        var ring = response.geometry().coordinates().getFirst();

        assertThat(response.mode()).isEqualTo("walk");
        assertThat(response.metadata().estimatedDistanceMeters()).isEqualTo(800);
        assertThat(response.metadata().calculationMethod()).isEqualTo("VIRTUAL_RADIAL_POLYGON");
        assertThat(ring).hasSize(37);
        assertThat(ring.getFirst()).isEqualTo(ring.getLast());
    }

    /**
     * Verifies that an unsupported travel duration raises the expected domain error.
     */
    @Test
    void rejectsUnsupportedIsochroneTime() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        assertThatThrownBy(() -> service.getIsochrone(1L, "walk", 12))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(PropertyErrorCode.INVALID_ISOCHRONE_PARAMETER);
    }
}
