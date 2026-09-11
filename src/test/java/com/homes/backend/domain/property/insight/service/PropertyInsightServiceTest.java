package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.insight.data.DobongAiDataset;
import com.homes.backend.domain.property.insight.dto.AiEvaluationRespDto;
import com.homes.backend.domain.property.insight.dto.IsochroneRespDto;
import com.homes.backend.domain.property.insight.repository.PropertyAiEvaluationRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyInsightServiceTest {
    @Mock PropertyRepository propertyRepository;
    @Mock PropertyAiEvaluationRepository evaluationRepository;
    @Mock DobongAiDataset dobongAiDataset;

    private PropertyInsightService service;
    private Property property;

    /**
     * Creates the service under test and a property with a valid coordinate.
     */
    @BeforeEach
    void setUp() {
        service = new PropertyInsightService(propertyRepository, evaluationRepository, dobongAiDataset);
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
     * Verifies that missing evaluation sources produce six pending categories.
     */
    @Test
    void returnsSixPendingCategoriesWhenGeospatialScoresAreNotLoaded() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(evaluationRepository.findById(1L)).thenReturn(Optional.empty());
        when(dobongAiDataset.findByAddress(property.getAddress())).thenReturn(Optional.empty());

        AiEvaluationRespDto response = service.getAiEvaluation(1L);

        assertThat(response.categories()).hasSize(6);
        assertThat(response.categories()).allMatch(category -> category.status() == AiEvaluationRespDto.ScoreStatus.PENDING_DATA);
        assertThat(response.overall().rawScore()).isNull();
        assertThat(response.overall().displayScore()).isNull();
        assertThat(response.overall().completeness()).isZero();
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
