package com.homes.backend.domain.property.price.controller;

import com.homes.backend.domain.property.price.dto.PropertyPricePredictionRespDto;
import com.homes.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "매물 가격 예측 API", description = "공공 실거래가 기반의 설명 가능한 가격 예측 API")
public interface PropertyPricePredictionControllerDocs {
    @Operation(summary = "매물 가격 예측", description = "최근 유사 아파트 매매 실거래를 이용해 다음 거래 예상 가격과 범위를 계산합니다. 금액 단위는 만원입니다. IP당 분당 30회로 제한되며 초과 시 HTTP 429를 반환합니다.")
    ApiResponse<PropertyPricePredictionRespDto> predict(@PathVariable Long propertyId, HttpServletRequest request);
}
