package com.homes.backend.domain.property.repository;

public interface PropertyDistanceProjection {
    Long getId();
    String getAddress();
    String getDetailAddress();
    String getTradeType();
    Long getDeposit();
    Long getMonthlyRent();
    Double getDesiredBrokerageFee();
    Double getDistanceInMeters();
}
