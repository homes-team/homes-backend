package com.homes.backend.domain.property.dto.request;

import com.homes.backend.domain.property.entity.PropertyOption;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.entity.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "매물 등록 요청 DTO")
public record PropertyCreateReqDto(
        @Schema(description = "거래 종류 (MONTHLY_RENT, JEONSE, SALE)", example = "MONTHLY_RENT")
        TradeType tradeType,

        @Schema(description = "방 종류 (ONE_ROOM, TWO_ROOM, VILLA, APARTMENT 등)", example = "ONE_ROOM")
        PropertyType propertyType,

        @Schema(description = "보증금/전세가/매매가 (단위: 만원)", example = "1000")
        Long deposit,

        @Schema(description = "월세 (단위: 만원, 전세/매매일 경우 0)", example = "80")
        Long monthlyRent,

        @Schema(description = "관리비 (단위: 만원)", example = "7")
        Long maintenanceFee,

        @Schema(description = "카카오 API 등에서 검색된 기본 주소", example = "서울 강남구 역삼동 123-45")
        String address,

        @Schema(description = "사용자가 직접 입력하는 상세 주소", example = "101동 502호")
        String detailAddress,

        @Schema(description = "해당 층수", example = "2")
        Integer currentFloor,

        @Schema(description = "건물 전체 층수", example = "5")
        Integer totalFloors,

        @Schema(description = "면적 (m²)", example = "19.95")
        Double area,

        @Schema(description = "사용자 작성 한 줄 소개글", example = "채광 좋고 깨끗한 남향 방입니다!")
        String description,

        @Schema(description = "희망 중개 수수료 비율", example = "0.3")
        Double desiredBrokerageFee,

        @Schema(description = "매물 옵션 리스트", example = "[\"AIR_CONDITIONER\", \"PARKING\"]")
        List<PropertyOption> options,

        @Schema(description = "지도 API에서 추출된 위도(Latitude)", example = "37.4979")
        Double latitude,

        @Schema(description = "지도 API에서 추출된 경도(Longitude)", example = "127.0276")
        Double longitude,

        @Schema(description = "매물 사진 URL 목록. GET /properties/presigned-url로 미리 S3에 업로드한 뒤 그 결과 URL들을 순서대로 담는다 (첫 번째가 대표 이미지)")
        List<String> imageUrls
) {
}
