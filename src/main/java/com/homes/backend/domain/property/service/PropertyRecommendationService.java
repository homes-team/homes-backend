package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.property.repository.RecentViewRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyRecommendationService {
    private final PropertyRepository propertyRepository;
    private final RecentViewRepository recentViewRepository; // 2. 의존성 주입 추가

    public List<PropertyListRespDto> getRecommendations(Long userId, Double minPrice, Double maxPrice, String preferredRegion) {
        /*
         * 비로그인 유저 허용 X
         */
        if (userId == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        // 유저의 최근 본 방 기록(Top 20)을 조회하여 추출
        List<Long> recentViewedIds = recentViewRepository.findTop20ByUserIdAndPropertyStatusNotOrderByViewedAtDesc(userId, PropertyStatus.DELETED)
                .stream()
                .map(recentView -> recentView.getProperty().getId())
                .toList();

        // 하이브리드 추천 repository 호출 시 recentViewedIds 전달 (상위 10개)
        List<Property> recommendedProperties = propertyRepository
                .findHybridRecommendations(minPrice, maxPrice, preferredRegion, recentViewedIds, 10);

        return recommendedProperties.stream()
                .map(PropertyListRespDto::from)
                .toList();
    }
}
