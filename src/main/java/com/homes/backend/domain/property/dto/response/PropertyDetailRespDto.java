package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.property.entity.*;

import java.util.List;

public record PropertyDetailRespDto(
        Long propertyId,
        List<String> imageUrls,
        String title,
        String description,
        String address,
        String detailAddress,
        TradeType tradeType,
        PropertyType propertyType,
        Long deposit,
        Long monthlyRent,
        Long maintenanceFee,
        Integer totalFloors,
        Integer currentFloor,
        PropertyDirection direction,
        Integer remodelingYear,
        Double area,
        Integer aiScore,
        Double desiredBrokerageFee,
        List<PropertyOption> options,
        String nearestStation,
        Integer walkingTime,
        Double latitude,
        Double longitude,
        Integer favoriteCount,
        boolean isSuspicious,
        PropertyStatus status
) {
    public static PropertyDetailRespDto from(Property property) {
        List<String> urls = property.getImages().stream()
                .map(PropertyImage::getImageUrl)
                .toList();

        return new PropertyDetailRespDto(
                property.getId(),
                urls,
                property.getTitle(),
                property.getDescription(),
                property.getAddress(),
                property.getDetailAddress(), // 상세 주소는 상세 페이지에서만 공개
                property.getTradeType(),
                property.getPropertyType(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getTotalFloors(),
                property.getCurrentFloor(),
                property.getDirection(),
                property.getRemodelingYear(),
                property.getArea(),
                property.getAiScore(),
                property.getDesiredBrokerageFee(),
                property.getOptions(),
                property.getNearestStation(),
                property.getWalkingTime(),
                property.getCoordinate().getY(), // 위도(Latitude)
                property.getCoordinate().getX(),  // 경도(Longitude)
                property.getFavoriteCount(),
                property.isSuspicious(),
                property.getStatus()
        );
    }

    /**
     * 삭제된 매물을 관리자가 아닌 사용자가 조회할 때 쓰는 응답. 사진/정확한 위치/설명 등 노출할 이유가 없는
     * 정보는 다 가리고, "삭제된 매물입니다"를 안내하는 데 필요한 최소 정보(제목, 상태)만 남긴다.
     */
    public static PropertyDetailRespDto deleted(Property property) {
        return new PropertyDetailRespDto(
                property.getId(),
                null,
                property.getTitle(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                property.getStatus()
        );
    }
}
