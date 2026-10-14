package com.homes.backend.domain.property.dto.request;

import com.homes.backend.domain.property.entity.PropertyOption;
import com.homes.backend.domain.property.entity.PropertySortType;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.entity.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record PropertyMapSearchReqDto(
        /**
         * 지도 영역 좌표
         */
        @NotNull(message = "남서쪽 위도는 필수입니다.")
        @Schema(description = "남서쪽 위도 (Y)", example = "37.4800")
        Double swLat,
        @NotNull(message = "남서쪽 경도는 필수입니다.")
        @Schema(description = "남서쪽 경도 (X)", example = "127.0100")
        Double swLng,
        @NotNull(message = "북동쪽 위도는 필수입니다.")
        @Schema(description = "북동쪽 위도 (Y)", example = "37.5100")
        Double neLat,
        @NotNull(message = "북동쪽 경도는 필수입니다.")
        @Schema(description = "북동쪽 경도 (X)", example = "127.0500")
        Double neLng,

        /**
         * 기본 필터
         */
        @Schema(description = "검색어 (주소 · 제목 · 태그 부분 일치)", example = "역삼동")
        String keyword,

        @PositiveOrZero(message = "면적은 0 이상이어야 합니다.")
        @Schema(description = "최소 전용면적(m²)", example = "20")
        Double minArea,

        @PositiveOrZero(message = "면적은 0 이상이어야 합니다.")
        @Schema(description = "최대 전용면적(m²)", example = "60")
        Double maxArea,

        @Schema(description = "거래 종류", example = "MONTHLY_RENT")
        TradeType tradeType,

        @Schema(description = "방 종류", example = "ONE_ROOM")
        PropertyType propertyType,


        /**
         * 가격대 필터
         */
        @PositiveOrZero(message = "보증금은 0 이상이어야 합니다.")
        @Schema(description = "최소 보증금(만원)", example = "500")
        Integer minDeposit,

        @PositiveOrZero(message = "보증금은 0 이상이어야 합니다.")
        @Schema(description = "최대 보증금(만원)", example = "2000")
        Integer maxDeposit,

        @PositiveOrZero(message = "월세는 0 이상이어야 합니다.")
        @Schema(description = "최소 월세(만원)", example = "40")
        Integer minMonthlyRent,

        @PositiveOrZero(message = "월세는 0 이상이어야 합니다.")
        @Schema(description = "최대 월세(만원)", example = "100")
        Integer maxMonthlyRent,

        /**
         * 추가 필터 (예: 주차가능, 엘리베이터)
         */
        @Schema(description = "옵션 필터 (모두 만족, AND 조건)")
        List<PropertyOption> options,

        /**
         * 정렬 조건
         */
        @Schema(description = "정렬 기준 (RECOMMENDED: 추천순, LATEST: 최신순, FAVORITE: 찜많은순)", example = "FAVORITE")
        PropertySortType sortBy
) {

    /**
     *  검증: 최소값이 최대값보다 클 수 없도록 방어
     */
    @AssertTrue(message = "최소값이 최대값보다 클 수 없습니다.")
    public boolean isValidRange() {
        if (minDeposit != null && maxDeposit != null && minDeposit > maxDeposit) return false;
        if (minMonthlyRent != null && maxMonthlyRent != null && minMonthlyRent > maxMonthlyRent) return false;
        if (minArea != null && maxArea != null && minArea > maxArea) return false;
        return true;
    }

    /**
     * 검증: 지도 좌표 역전 방어 (남서쪽은 북동쪽보다 작아야 함)
     */
    @AssertTrue(message = "지도 좌표가 유효하지 않거나 WGS84 범위를 벗어났습니다. (위도: -90~90, 경도: -180~180)")
    public boolean isValidCoordinate() {
        // 역전 방어 (남서쪽은 북동쪽보다 작아야 함)
        if (swLat != null && neLat != null && swLat >= neLat) return false;
        if (swLng != null && neLng != null && swLng >= neLng) return false;

        // WGS84 유효 범위 검증 (위도: -90 ~ 90)
        if (swLat != null && (swLat < -90 || swLat > 90)) return false;
        if (neLat != null && (neLat < -90 || neLat > 90)) return false;

        // WGS84 유효 범위 검증 (경도: -180 ~ 180)
        if (swLng != null && (swLng < -180 || swLng > 180)) return false;
        if (neLng != null && (neLng < -180 || neLng > 180)) return false;

        return true;
    }
}
