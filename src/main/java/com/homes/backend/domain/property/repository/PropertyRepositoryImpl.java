package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.*;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import com.homes.backend.domain.property.entity.PropertyOption;

import java.util.List;

import static com.homes.backend.domain.property.entity.QProperty.property;

@Repository
@RequiredArgsConstructor
public class PropertyRepositoryImpl implements PropertyRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private static final double AI_SCORE_MAX = 100.0;
    private static final double FAVORITE_CAP = 300.0; // 찜 300개 이상은 만점 처리

    /**
     * ====================== 하이브리드 추천 및 개인화 필터링 메인 로직 ======================
     *
     * 1. 하이브리드 점수 산정:
     *    - AI 추천 매물 점수 (aiScore, 50%): 모델이 분석한 객관적 추천 지표
     *    - 유저 찜하기 횟수 (favoriteCount, 45%): 대중적으로 검증된 인기도 지표
     *    - 최근 본 방 가중치 보너스 (5%): 단순 클릭으로 인한 노이즈를 고려하여 가중치를 낮게 설정하되,
     *      유저가 최근에 조회했던 매물일 경우 점수를 추가로 가산하여 개인화 체감도를 높임.
     *
     * 2. 필수 조건:
     *    - 사용자의 검색 필터 값과 상관없이, 조회되는 매물은 무조건 '거래 가능한 상태(AVAILABLE)'이면서 '허위 매물로 신고되거나 의심되지 않은 안전한 매물'이어야 합니다.
     *
     * 3. 동적 개인화 필터링:
     *    - 사용자가 입력한 검색 조건(최소/최대 가격, 선호 지역 등)에 따라 유연하게 쿼리를 조작합니다.
     *    - 값이 null이거나 빈 값일 경우 해당 조건은 쿼리에서 자동으로 무시(제외)되어 전체 매물 대상 조회로 유연하게 동작합니다.
     */
    @Override
    public List<Property> findHybridRecommendations(Double minPrice, Double maxPrice, String preferredRegion, List<Long> recentViewedIds, int limit) {

        return queryFactory
                .selectFrom(property)
                .where(
                        property.status.eq(PropertyStatus.AVAILABLE),
                        property.isSuspicious.eq(false),
                        property.reportCount.eq(0),
                        priceBetween(minPrice, maxPrice),
                        regionContains(preferredRegion)
                )
                .orderBy(recommendationScore(recentViewedIds).desc(), property.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * 점수 정규화 및 하이브리드 계산 메서드
     */
    private NumberExpression<Double> recommendationScore(List<Long> recentViewedIds) {
        // aiScore: NULL이면 0 처리 후 정규화 (가중치 0.5)
        NumberExpression<Double> aiPart = property.aiScore.coalesce(0).doubleValue()
                .divide(AI_SCORE_MAX)
                .multiply(0.5);

        // favoriteCount: 상한선(300)을 두어 정규화 (가중치 0.45)
        NumberExpression<Double> favPart = Expressions.numberTemplate(Double.class,
                        "case when {0} > {1} then 1.0 else {0} / {1} end",
                        property.favoriteCount, FAVORITE_CAP)
                .multiply(0.45);

        NumberExpression<Double> score = aiPart.add(favPart);

        // 최근 본 방 보너스 가산 (가중치 0.05)
        if (recentViewedIds != null && !recentViewedIds.isEmpty()) {
            score = score.add(Expressions.cases()
                    .when(property.id.in(recentViewedIds)).then(0.05)
                    .otherwise(0.0));
        }
        return score;
    }

    /**
     * sortBy 값에 따른 정렬 분기 메서드 추가
     */
    private OrderSpecifier<?>[] getOrderSpecifier(PropertySortType sortBy, List<Long> recentViewedIds) {
        if (sortBy == PropertySortType.RECOMMENDED) {
            return new OrderSpecifier[]{
                    recommendationScore(recentViewedIds).desc(),
                    property.id.desc()
            };
        }
        if (sortBy == PropertySortType.FAVORITE) {
            return new OrderSpecifier[]{
                    property.favoriteCount.desc(),
                    property.id.desc()
            };
        }
        if (sortBy == PropertySortType.LATEST) {
            return new OrderSpecifier[]{
                    property.id.desc()
            };
        }
        // 기본값 처리
        return new OrderSpecifier[]{
                recommendationScore(recentViewedIds).desc(),
                property.id.desc()
        };
    }

    /**
     * 동적 쿼리 - 가격 범위
     */
    private BooleanExpression priceBetween(Double minPrice, Double maxPrice) {
        if (minPrice == null && maxPrice == null) return null;
        if (minPrice != null && maxPrice == null) return property.deposit.goe(minPrice);
        if (minPrice == null && maxPrice != null) return property.deposit.loe(maxPrice);
        return property.deposit.between(minPrice, maxPrice);
    }

    /**
     * 동적 쿼리 - 선호 지역 포함 여부
     */
    private BooleanExpression regionContains(String preferredRegion) {
        if (preferredRegion == null || preferredRegion.isBlank()) return null;
        return property.address.contains(preferredRegion);
    }


    /**
     * ====================== 지도 기반 검색 및 다중 필터링 ======================
     */
    @Override
    public List<Property> findPropertiesByMapAndFilters(
            Polygon boundingBox, List<PropertyStatus> statuses, TradeType tradeType,
            PropertyType propertyType, Integer minDeposit, Integer maxDeposit,
            Integer minMonthlyRent, Integer maxMonthlyRent, Double minArea, Double maxArea,
            String keyword, List<PropertyOption> options, PropertySortType sortBy, List<Long> recentViewedIds
    ) {

        return queryFactory
                .selectFrom(property)
                .where(
                        withinBounds(boundingBox),
                        statusIn(statuses),
                        tradeTypeEq(tradeType),
                        propertyTypeEq(propertyType),
                        depositBetween(minDeposit, maxDeposit),
                        monthlyRentBetween(minMonthlyRent, maxMonthlyRent),
                        areaBetween(minArea, maxArea),
                        keywordMatches(keyword),
                        optionsContainAll(options)
                )
                .orderBy(getOrderSpecifier(sortBy, recentViewedIds))
                .limit(300)
                .fetch();
    }

    /**
     * --- 동적 쿼리 블록 (지도 검색용) ---
      */
    private BooleanExpression withinBounds(Polygon boundingBox) {
        if (boundingBox == null) return null;
        return Expressions.booleanTemplate("function('ST_Contains', {0}, {1}) = true", boundingBox, property.coordinate);
    }

    private BooleanExpression statusIn(List<PropertyStatus> statuses) {
        return (statuses != null && !statuses.isEmpty()) ? property.status.in(statuses) : null;
    }

    private BooleanExpression tradeTypeEq(TradeType tradeType) {
        return tradeType != null ? property.tradeType.eq(tradeType) : null;
    }

    private BooleanExpression propertyTypeEq(PropertyType propertyType) {
        return propertyType != null ? property.propertyType.eq(propertyType) : null;
    }

    private BooleanExpression depositBetween(Integer min, Integer max) {
        if (min != null && max != null) return property.deposit.between(min, max);
        if (min != null) return property.deposit.goe(min);
        if (max != null) return property.deposit.loe(max);
        return null;
    }

    private BooleanExpression monthlyRentBetween(Integer min, Integer max) {
        if (min != null && max != null) return property.monthlyRent.between(min, max);
        if (min != null) return property.monthlyRent.goe(min);
        if (max != null) return property.monthlyRent.loe(max);
        return null;
    }

    private BooleanExpression areaBetween(Double min, Double max) {
        if (min != null && max != null) {
            return property.area.between(min, max);
        }
        if (min != null) {
            return property.area.goe(min);
        }
        if (max != null) {
            return property.area.loe(max);
        }
        return null; // 조건이 안 들어오면 전체 검색(무시)
    }


    private BooleanExpression keywordMatches(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;

        String likeKeyword = "%" + keyword + "%";
        BooleanExpression expression = property.address.like(likeKeyword)
                .or(property.title.like(likeKeyword));

        // 입력한 검색어(예: "에어컨")가 PropertyOption의 한글 설명과 일치하면 OR 조건으로 추가
        if (keyword.length() >= 2) {
            for (PropertyOption option : PropertyOption.values()) {
                if (option.getDescription().contains(keyword)) {
                    expression = expression.or(property.options.any().eq(option));
                }
            }
        }

        return expression;
    }

    private BooleanExpression optionsContainAll(List<PropertyOption> options) {
        if (options == null || options.isEmpty()) return null;

        BooleanExpression expr = null;
        for (PropertyOption option : options) {
            BooleanExpression cond = property.options.any().eq(option);
            expr = (expr == null) ? cond : expr.and(cond);
        }
        return expr;
    }

}
