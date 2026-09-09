package com.homes.backend.domain.property.dto.request;

import com.homes.backend.domain.property.entity.PropertyOption;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.entity.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "매물 수정 요청 DTO")
public record PropertyUpdateReqDto (
        @Schema(description = "거래 종류", example = "JEONSE") TradeType tradeType,
        @Schema(description = "방 종류", example = "APARTMENT") PropertyType propertyType,
        @Schema(description = "보증금/전세가/매매가 (단위: 만원)", example = "120000") Long deposit,
        @Schema(description = "월세 (단위: 만원)", example = "0") Long monthlyRent,
        @Schema(description = "관리비 (단위: 만원)", example = "20") Long maintenanceFee,
        @Schema(description = "주소", example = "서울 도봉구 하하동 123-45") String address,
        @Schema(description = "상세 주소", example = "101동 502호") String detailAddress,
        @Schema(description = "해당 층수", example = "2") Integer currentFloor,
        @Schema(description = "전체 층수", example = "5") Integer totalFloors,
        @Schema(description = "면적 (m²)", example = "19.95") Double area,
        @Schema(description = "매물 한 줄 소개", example = "가격 내렸습니다! 컨디션 좋아요.") String description,
        @Schema(description = "희망 중개 수수료", example = "0.3") Double desiredBrokerageFee,
        @Schema(description = "매물 옵션 리스트", example = "[\"AIR_CONDITIONER\", \"PARKING\"]") List<PropertyOption> options,
        @Schema(description = "위도(Latitude)", example = "37.4979") Double latitude,
        @Schema(description = "경도(Longitude)", example = "127.0276") Double longitude,
        @Schema(description = "새 매물 사진 URL 목록(선택). 보내면 기존 사진을 전부 교체한다. GET /properties/presigned-url로 미리 업로드 후 그 결과 URL을 담는다.")
        List<String> newImageUrls
){ }
