package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropertyRankingService {
    private final StringRedisTemplate redisTemplate;
    private final PropertyRepository propertyRepository;

    private static final String RANKING_KEY = "property:ranking";

    /**
     * 가중치 상수 정의
     */
    private static final double VIEW_SCORE = 1.0;
    private static final double FAVORITE_SCORE = 5.0;

    /**
     * 상세 조회 시 점수 증가 (+1점)
     */
    public void incrementViewScore(Long propertyId) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, String.valueOf(propertyId), 1);
    }

    /**
     * 찜하기 등록 시 점수 증가 (+5점)
     */
    public void incrementFavoriteScore(Long propertyId) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, String.valueOf(propertyId), FAVORITE_SCORE);
    }

    /**
     * 찜하기 취소 시 점수 감소 (-5점)
     */
    public void decrementFavoriteScore(Long propertyId) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, String.valueOf(propertyId), -FAVORITE_SCORE);
    }

    /**
     * 급등 매물 TOP 10 조회 (가중치 반영된 순위)
     */
    @Transactional(readOnly = true)
    public List<PropertyListRespDto> getSurgeRankings() {
        /*
         * ZREVRANGE를 이용해 가중치 합산 점수가 높은 순으로 상위 10개 조회
         */
        Set<String> typedTuples = redisTemplate.opsForZSet().reverseRange(RANKING_KEY, 0, 9);

        if (typedTuples == null || typedTuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> propertyIds = typedTuples.stream()
                .map(Long::valueOf)
                .toList();

        List<Property> properties = propertyRepository.findByIdInAndStatusNot(propertyIds, PropertyStatus.DELETED);

        /*
         * Redis 순서(인기순)대로 정렬
         */
        return propertyIds.stream()
                .map(id -> properties.stream()
                        .filter(p -> p.getId().equals(id))
                        .findFirst()
                        .map(PropertyListRespDto::from)
                        .orElse(null))
                .filter(dto -> dto != null)
                .toList();
    }
}
