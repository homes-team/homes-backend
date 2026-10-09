package com.homes.backend.domain.property.insight.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto.Facility;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto.FacilitySource;
import com.homes.backend.domain.property.insight.dto.NearbyFacilitiesRespDto.FacilityType;
import com.homes.backend.domain.property.repository.NearbyStationProjection;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.property.repository.StationRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NearbyFacilityService {
    public static final int DEFAULT_RADIUS_METERS = 2_000;
    static final int MIN_RADIUS_METERS = 100;
    static final int MAX_RADIUS_METERS = 5_000;
    static final int MAX_RESULTS_PER_TYPE = 5;
    private static final double METERS_PER_DEGREE = 111_320.0;
    private static final String SUBWAY_POI_TYPE = "지하철역";
    private static final String BUS_STOP_POI_TYPE = "버스정류장";

    private final PropertyRepository propertyRepository;
    private final StationRepository stationRepository;
    private final KakaoNearbyFacilityProvider kakaoNearbyFacilityProvider;

    public NearbyFacilitiesRespDto getNearbyFacilities(
            Long propertyId,
            int radiusMeters,
            Set<FacilityType> requestedTypes
    ) {
        if (radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        Point center = property.getCoordinate();
        if (center == null || !Double.isFinite(center.getX()) || !Double.isFinite(center.getY())) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        Set<FacilityType> types = requestedTypes == null || requestedTypes.isEmpty()
                ? EnumSet.allOf(FacilityType.class)
                : EnumSet.copyOf(requestedTypes);
        List<Facility> facilities = new ArrayList<>();

        if (types.contains(FacilityType.SUBWAY)) {
            facilities.addAll(findTransport(center, FacilityType.SUBWAY, SUBWAY_POI_TYPE, radiusMeters));
        }
        if (types.contains(FacilityType.BUS_STOP)) {
            facilities.addAll(findTransport(center, FacilityType.BUS_STOP, BUS_STOP_POI_TYPE, radiusMeters));
        }
        for (FacilityType type : List.of(
                FacilityType.SCHOOL,
                FacilityType.HOSPITAL,
                FacilityType.CULTURE,
                FacilityType.PARK,
                FacilityType.MART
        )) {
            if (!types.contains(type)) continue;
            facilities.addAll(kakaoNearbyFacilityProvider
                    .findNearby(center, type, radiusMeters, MAX_RESULTS_PER_TYPE)
                    .stream()
                    .map(place -> new Facility(
                            "KAKAO-" + place.id(),
                            type,
                            place.name(),
                            place.latitude(),
                            place.longitude(),
                            place.distanceMeters(),
                            place.address(),
                            FacilitySource.KAKAO_LOCAL
                    ))
                    .toList());
        }

        facilities.sort(Comparator.comparingLong(Facility::distanceMeters));
        return new NearbyFacilitiesRespDto(
                propertyId,
                new NearbyFacilitiesRespDto.Center(center.getY(), center.getX()),
                radiusMeters,
                List.copyOf(facilities)
        );
    }

    private List<Facility> findTransport(
            Point center,
            FacilityType type,
            String poiType,
            int radiusMeters
    ) {
        return stationRepository.findNearbyByPoiType(
                        center,
                        poiType,
                        radiusMeters,
                        radiusMeters / METERS_PER_DEGREE,
                        MAX_RESULTS_PER_TYPE
                ).stream()
                .filter(this::hasValidCoordinate)
                .map(station -> new Facility(
                        "TRANSPORT-" + (station.getPoiId() == null ? station.getId() : station.getPoiId()),
                        type,
                        station.getPoiName(),
                        station.getLatitude(),
                        station.getLongitude(),
                        Math.round(station.getDistance()),
                        null,
                        FacilitySource.NATIONWIDE_TRANSPORT_POI
                ))
                .toList();
    }

    private boolean hasValidCoordinate(NearbyStationProjection station) {
        return station.getLatitude() != null && Double.isFinite(station.getLatitude())
                && station.getLongitude() != null && Double.isFinite(station.getLongitude())
                && station.getDistance() != null && Double.isFinite(station.getDistance());
    }
}
