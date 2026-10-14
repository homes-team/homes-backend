package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.*;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

public interface PropertyRepositoryCustom {
    List<Property> findHybridRecommendations(Double minPrice, Double maxPrice, String preferredRegion, List<Long> recentViewedIds, int limit);

    /**
     * 지도 기반 검색 및 필터링 전용 메서드
      */
    List<Property> findPropertiesByMapAndFilters(
            Polygon boundingBox,
            List<PropertyStatus> statuses,
            TradeType tradeType,
            PropertyType propertyType,
            Integer minDeposit,
            Integer maxDeposit,
            Integer minMonthlyRent,
            Integer maxMonthlyRent,
            Double minArea,
            Double maxArea,
            String keyword,
            List<PropertyOption> options,
            PropertySortType sortBy,
            List<Long> recentViewedIds
    );
}
