package com.homes.backend.domain.property.repository;

public interface StationDistanceProjection {
    String getPoiName(); // 지하철역 이름
    Double getDistance(); // 거리(m)
}
