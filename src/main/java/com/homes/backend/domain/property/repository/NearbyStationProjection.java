package com.homes.backend.domain.property.repository;

public interface NearbyStationProjection {
    Long getId();
    String getPoiId();
    String getPoiName();
    String getPoiType();
    Double getLatitude();
    Double getLongitude();
    Double getDistance();
}
