package com.homes.backend.domain.property.registry.controller;

import com.homes.backend.domain.property.registry.dto.response.PropertyRegistryRiskResDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "매물 등기부등본(Registry Risk) API", description = "매물 권리관계(저당권/가압류) 위험도 스캔 결과 조회 API")
public interface PropertyRegistryRiskControllerDocs {

    @Operation(summary = "등기부등본 위험도 스캔 결과 조회", description = "매물 등록/수정 시 백그라운드로 실행된 등기부등본(권리관계) 위험도 스캔 결과를 조회합니다. " +
            "저당권/가압류 건수와 위험도 등급(SAFE/CAUTION/DANGER)을 반환하며, DANGER 등급은 자동으로 의심 매물로도 전환됩니다. " +
            "아직 스캔이 완료되지 않았으면 404를 반환합니다. " +
            "(실제 등기부등본 조회는 개인 개발자가 접근 가능한 공개 API가 없어, 현재는 매물 ID 기반의 결정적인 mock 데이터로 동작합니다.)")
    ApiResponse<PropertyRegistryRiskResDto> getRegistryRisk(
            @Parameter(description = "조회할 매물의 ID", required = true) @PathVariable Long propertyId
    );
}
