package com.homes.backend.domain.realtor.dto.response;

import com.homes.backend.domain.property.entity.TradeType;
import com.homes.backend.domain.property.repository.PropertyDistanceProjection;

public record NearbyPropertyResDto(
        Long propertyId,
        String address,
        String detailAddress,
        TradeType tradeType,
        Long deposit,
        Long monthlyRent,
        Double desiredBrokerageFee,
        Double distanceInMeters
) {
    public static NearbyPropertyResDto from(PropertyDistanceProjection projection) {
        return new NearbyPropertyResDto(
                projection.getId(),
                projection.getAddress(),
                projection.getDetailAddress(),
                projection.getTradeType() != null ? TradeType.valueOf(projection.getTradeType()) : null,
                projection.getDeposit(),
                projection.getMonthlyRent(),
                projection.getDesiredBrokerageFee(),
                projection.getDistanceInMeters()
        );
    }
}
