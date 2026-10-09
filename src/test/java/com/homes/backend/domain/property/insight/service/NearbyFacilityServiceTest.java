package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto.FacilityType;
import com.homes.backend.domain.property.repository.NearbyStationProjection;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.property.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NearbyFacilityServiceTest {

    @Mock PropertyRepository propertyRepository;
    @Mock StationRepository stationRepository;
    @Mock KakaoNearbyFacilityProvider kakaoNearbyFacilityProvider;
    @InjectMocks NearbyFacilityService service;

    @Test
    void combinesTransportAndKakaoFacilitiesOrderedByDistance() {
        Property property = mock(Property.class);
        var center = new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688));
        when(property.getCoordinate()).thenReturn(center);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));

        NearbyStationProjection subway = mock(NearbyStationProjection.class);
        when(subway.getPoiId()).thenReturn("S112");
        when(subway.getPoiName()).thenReturn("방학역");
        when(subway.getLatitude()).thenReturn(37.6674);
        when(subway.getLongitude()).thenReturn(127.0443);
        when(subway.getDistance()).thenReturn(420.0);
        when(stationRepository.findNearbyByPoiType(
                eq(center), eq("지하철역"), eq(2_000.0), anyDouble(), eq(5)))
                .thenReturn(List.of(subway));
        when(kakaoNearbyFacilityProvider.findNearby(
                eq(center), eq(FacilityType.HOSPITAL), eq(2_000), eq(5)))
                .thenReturn(List.of(new KakaoNearbyFacilityProvider.Place(
                        "hospital-1", "한일병원", 37.6690, 127.0475, 240, "서울 도봉구 테스트로 1")));

        var result = service.getNearbyFacilities(
                21L, 2_000, Set.of(FacilityType.SUBWAY, FacilityType.HOSPITAL));

        assertThat(result.center().latitude()).isEqualTo(37.6688);
        assertThat(result.facilities()).extracting(facility -> facility.type())
                .containsExactly(FacilityType.HOSPITAL, FacilityType.SUBWAY);
        assertThat(result.facilities()).extracting(facility -> facility.distanceMeters())
                .containsExactly(240L, 420L);
    }

    @Test
    void defaultsToAllFacilityTypesWhenFilterIsEmpty() {
        Property property = mock(Property.class);
        var center = new GeometryFactory().createPoint(new Coordinate(127.0471, 37.6688));
        when(property.getCoordinate()).thenReturn(center);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(stationRepository.findNearbyByPoiType(any(), any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());
        when(kakaoNearbyFacilityProvider.findNearby(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        var result = service.getNearbyFacilities(1L, 2_000, Set.of());

        assertThat(result.facilities()).isEmpty();
    }
}
