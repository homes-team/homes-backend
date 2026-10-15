package com.homes.backend.domain.property.insight.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class NearbyFacilityService {
    public static final int DEFAULT_RADIUS_METERS = 2_000;
    static final int MIN_RADIUS_METERS = 100;
    static final int MAX_RADIUS_METERS = 5_000;
    static final int MAX_RESULTS_PER_TYPE = 5;
    private static final long KAKAO_LOOKUP_TIMEOUT_SECONDS = 4;
    private static final String SUBWAY_POI_TYPE = "지하철역";
    private static final String BUS_STOP_POI_TYPE = "버스정류장";
    private static final List<FacilityType> KAKAO_FACILITY_TYPES = List.of(
            FacilityType.SCHOOL,
            FacilityType.HOSPITAL,
            FacilityType.CULTURE,
            FacilityType.PARK,
            FacilityType.MART
    );

    private final PropertyRepository propertyRepository;
    private final StationRepository stationRepository;
    private final KakaoNearbyFacilityProvider kakaoNearbyFacilityProvider;
    private final Executor nearbyFacilityTaskExecutor;
    private final Cache<RequestKey, NearbyFacilitiesRespDto> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public NearbyFacilityService(
            PropertyRepository propertyRepository,
            StationRepository stationRepository,
            KakaoNearbyFacilityProvider kakaoNearbyFacilityProvider,
            @Qualifier("nearbyFacilityTaskExecutor") Executor nearbyFacilityTaskExecutor
    ) {
        this.propertyRepository = propertyRepository;
        this.stationRepository = stationRepository;
        this.kakaoNearbyFacilityProvider = kakaoNearbyFacilityProvider;
        this.nearbyFacilityTaskExecutor = nearbyFacilityTaskExecutor;
    }

    public NearbyFacilitiesRespDto getNearbyFacilities(
            Long propertyId,
            int radiusMeters,
            Set<FacilityType> requestedTypes
    ) {
        if (radiusMeters < MIN_RADIUS_METERS || radiusMeters > MAX_RADIUS_METERS) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        Set<FacilityType> types = requestedTypes == null || requestedTypes.isEmpty()
                ? EnumSet.allOf(FacilityType.class)
                : EnumSet.copyOf(requestedTypes);
        RequestKey cacheKey = new RequestKey(propertyId, radiusMeters, Set.copyOf(types));
        return cache.get(cacheKey, key -> loadNearbyFacilities(
                key.propertyId(), key.radiusMeters(), key.types()));
    }

    private NearbyFacilitiesRespDto loadNearbyFacilities(
            Long propertyId,
            int radiusMeters,
            Set<FacilityType> types
    ) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        Point center = property.getCoordinate();
        if (center == null || !Double.isFinite(center.getX()) || !Double.isFinite(center.getY())) {
            throw new CustomException(GlobalErrorCode.INVALID_INPUT);
        }

        List<Facility> facilities = new ArrayList<>();

        if (types.contains(FacilityType.SUBWAY)) {
            facilities.addAll(findTransport(center, FacilityType.SUBWAY, SUBWAY_POI_TYPE, radiusMeters));
        }
        if (types.contains(FacilityType.BUS_STOP)) {
            facilities.addAll(findTransport(center, FacilityType.BUS_STOP, BUS_STOP_POI_TYPE, radiusMeters));
        }
        facilities.addAll(findKakaoFacilities(center, radiusMeters, types));

        facilities.sort(Comparator.comparingLong(Facility::distanceMeters));
        return new NearbyFacilitiesRespDto(
                propertyId,
                new NearbyFacilitiesRespDto.Center(center.getY(), center.getX()),
                radiusMeters,
                List.copyOf(facilities)
        );
    }

    private List<Facility> findKakaoFacilities(
            Point center,
            int radiusMeters,
            Set<FacilityType> types
    ) {
        List<CompletableFuture<List<Facility>>> futures = KAKAO_FACILITY_TYPES.stream()
                .filter(types::contains)
                .map(type -> submitKakaoLookup(center, type, radiusMeters))
                .toList();
        if (futures.isEmpty()) return List.of();

        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .get(KAKAO_LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            log.warn("카카오 주변 시설 전체 조회 시간 초과: radiusMeters={}", radiusMeters);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("카카오 주변 시설 조회 중 요청 스레드 인터럽트: radiusMeters={}", radiusMeters);
        } catch (ExecutionException exception) {
            log.warn("카카오 주변 시설 병렬 조회 실패: radiusMeters={}", radiusMeters, exception.getCause());
        } finally {
            futures.stream().filter(future -> !future.isDone()).forEach(future -> future.cancel(true));
        }

        return futures.stream()
                .filter(future -> future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally())
                .flatMap(future -> future.join().stream())
                .toList();
    }

    private CompletableFuture<List<Facility>> submitKakaoLookup(
            Point center,
            FacilityType type,
            int radiusMeters
    ) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> kakaoNearbyFacilityProvider
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
                            .toList(),
                    nearbyFacilityTaskExecutor
            );
        } catch (RejectedExecutionException exception) {
            log.warn("카카오 주변 시설 조회 작업 거부: type={}", type);
            return CompletableFuture.completedFuture(List.of());
        }
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

    private record RequestKey(Long propertyId, int radiusMeters, Set<FacilityType> types) {}
}
