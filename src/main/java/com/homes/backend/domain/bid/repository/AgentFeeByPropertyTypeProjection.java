package com.homes.backend.domain.bid.repository;

import com.homes.backend.domain.property.entity.PropertyType;

public interface AgentFeeByPropertyTypeProjection {
    PropertyType getPropertyType();
    Double getAverageFee();
}
