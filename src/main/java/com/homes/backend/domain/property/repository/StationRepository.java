package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.locationtech.jts.geom.Point;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<Station, Long> {
    /**
     * 주어진 Point와 가장 가까운 역을 미터(m) 단위 거리와 함께 1개 반환
      */
    @Query(value = "SELECT poi_name AS poiName, " +
            "ST_DistanceSphere(coordinate, :point) AS distance " +
            "FROM station " +
            "WHERE poi_type = '지하철역' " +
            "ORDER BY distance ASC LIMIT 1", nativeQuery = true)
    StationDistanceProjection findNearestStationWithDistance(@Param("point") Point point);

    /**
     * Finds the nearest transportation POI of the requested type.
     */
    @Query(value = "SELECT poi_name AS poiName, " +
            "ST_DistanceSphere(coordinate, :point) AS distance " +
            "FROM station " +
            "WHERE poi_type = :poiType " +
            "AND ST_DWithin(coordinate, :point, :maxDistanceDegrees) " +
            "ORDER BY distance ASC LIMIT 1", nativeQuery = true)
    StationDistanceProjection findNearestByPoiType(
            @Param("point") Point point,
            @Param("poiType") String poiType,
            @Param("maxDistanceDegrees") double maxDistanceDegrees
    );

    /**
     * Counts transportation POIs of one type within a radius of the supplied coordinate.
     */
    @Query(value = "SELECT COUNT(*) FROM station " +
            "WHERE poi_type = :poiType " +
            "AND ST_DWithin(coordinate, :point, :radiusDegrees) " +
            "AND ST_DistanceSphere(coordinate, :point) <= :radiusMeters", nativeQuery = true)
    long countByPoiTypeWithinRadius(
            @Param("point") Point point,
            @Param("poiType") String poiType,
            @Param("radiusMeters") double radiusMeters,
            @Param("radiusDegrees") double radiusDegrees
    );

    boolean existsByPoiType(String poiType);
}
