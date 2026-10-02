package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.property.entity.*;

import java.time.LocalDateTime;
import java.util.List;

public record PropertyListRespDto(
        Long propertyId,
        String thumbnailUrl,
        String title,
        String address,
        Double latitude,
        Double longitude,
        PropertyType propertyType,
        TradeType tradeType,
        Long deposit,
        Long monthlyRent,
        Long maintenanceFee,
        Integer totalFloors,
        Integer currentFloor,
        Double area,
        String description,
        LocalDateTime createdAt,
        Integer aiScore,
        List<PropertyOption> options,
        String nearestStation,
        Integer walkingTime,
        Integer favoriteCount,
        boolean isSuspicious,
        PropertyStatus status
) {
    public static PropertyListRespDto from(Property property) {
        /**
         * 여러 사진 중 대표 이미지(isThumbnail=true)만 찾아서 URL 추출
          */
        String thumbnail = property.getImages().stream()
                .filter(PropertyImage::getIsThumbnail)
                .map(PropertyImage::getImageUrl)
                .findFirst()
                .orElse(null); // 사진이 한 장도 없으면 null 반환

        return new PropertyListRespDto(
                property.getId(),
                thumbnail,
                property.getTitle(),
                property.getAddress(),
                property.getCoordinate().getY(),
                property.getCoordinate().getX(),
                property.getPropertyType(),
                property.getTradeType(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getTotalFloors(),
                property.getCurrentFloor(),
                property.getArea(),
                property.getDescription(),
                property.getCreatedAt(),
                property.getAiScore(),
                List.copyOf(property.getOptions()),
                property.getNearestStation(),
                property.getWalkingTime(),
                property.getFavoriteCount(),
                property.isSuspicious(),
                property.getStatus()
        );
    }
}
