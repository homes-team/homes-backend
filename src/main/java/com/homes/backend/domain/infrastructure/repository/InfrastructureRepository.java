package com.homes.backend.domain.infrastructure.repository;

import com.homes.backend.domain.infrastructure.entity.Infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {

    // ST_MakeEnvelope 함수로 Bounding Box를 만들고 &&연산자로 겹치는(포함되는) 마커만 추출
    @Query(value = "SELECT * FROM infrastructure WHERE " +
            "(:infraType IS NULL OR infra_type = :infraType) AND " +
            "location && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)",
            nativeQuery = true)
    List<Infrastructure> findInBoundingBox(
            @Param("minLon") Double minLon,
            @Param("minLat") Double minLat,
            @Param("maxLon") Double maxLon,
            @Param("maxLat") Double maxLat,
            @Param("infraType") String infraType // null이면 전체, 값이 있으면 해당 시설만 조회
    );
}
