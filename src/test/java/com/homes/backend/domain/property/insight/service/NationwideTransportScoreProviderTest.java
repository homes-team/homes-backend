package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.repository.StationDistanceProjection;
import com.homes.backend.domain.property.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NationwideTransportScoreProviderTest {

    @Test
    void calculatesNationwideScoreWithInspectableEvidence() {
        StationRepository repository = mock(StationRepository.class);
        NationwideTransportScoreProvider provider = new NationwideTransportScoreProvider(repository);
        Point coordinate = new GeometryFactory().createPoint(new Coordinate(129.0756, 35.1796));
        coordinate.setSRID(4326);

        StationDistanceProjection nearestSubway = mock(StationDistanceProjection.class);
        when(nearestSubway.getPoiName()).thenReturn("서면역");
        when(nearestSubway.getDistance()).thenReturn(420.0);
        StationDistanceProjection nearestBus = mock(StationDistanceProjection.class);
        when(nearestBus.getPoiName()).thenReturn("서면교차로");
        when(nearestBus.getDistance()).thenReturn(90.0);
        when(repository.findNearestByPoiType(eq(coordinate), eq("지하철역"), anyDouble()))
                .thenReturn(nearestSubway);
        when(repository.findNearestByPoiType(eq(coordinate), eq("버스정류장"), anyDouble()))
                .thenReturn(nearestBus);
        when(repository.countByPoiTypeWithinRadius(
                eq(coordinate), eq("지하철역"), anyDouble(), anyDouble()))
                .thenReturn(2L);
        when(repository.countByPoiTypeWithinRadius(
                eq(coordinate), eq("버스정류장"), anyDouble(), anyDouble()))
                .thenReturn(5L);

        NationwideTransportScoreProvider.Result result = provider.evaluate(coordinate).orElseThrow();

        assertThat(result.score()).isEqualTo(91.0);
        assertThat(result.description()).contains("서면역", "420m", "서면교차로", "90m", "2곳", "5곳");
        assertThat(result.evidence()).extracting(evidence -> evidence.code())
                .containsExactly("NEAREST_SUBWAY_DISTANCE", "NEAREST_BUS_DISTANCE",
                        "SUBWAY_COUNT_WITHIN_1KM", "BUS_COUNT_WITHIN_500M");
        assertThat(result.evidence().get(0).value()).isEqualTo("서면역 · 420m");
        assertThat(result.evidence().get(1).value()).isEqualTo("서면교차로 · 90m");
        assertThat(result.calculation().policyVersion())
                .isEqualTo(NationwideTransportScoreProvider.POLICY_VERSION);
    }

    @Test
    void removesDuplicatedStationSuffixFromDescriptionAndEvidence() {
        StationRepository repository = mock(StationRepository.class);
        NationwideTransportScoreProvider provider = new NationwideTransportScoreProvider(repository);
        Point coordinate = new GeometryFactory().createPoint(new Coordinate(127.044, 37.665));

        StationDistanceProjection nearestSubway = mock(StationDistanceProjection.class);
        when(nearestSubway.getPoiName()).thenReturn("방학역역");
        when(nearestSubway.getDistance()).thenReturn(1_345.0);
        when(repository.findNearestByPoiType(eq(coordinate), eq("지하철역"), anyDouble()))
                .thenReturn(nearestSubway);

        NationwideTransportScoreProvider.Result result = provider.evaluate(coordinate).orElseThrow();

        assertThat(result.description()).contains("방학역(약 1345m)").doesNotContain("방학역역");
        assertThat(result.evidence().get(0).value()).isEqualTo("방학역 · 1345m");
    }
}
