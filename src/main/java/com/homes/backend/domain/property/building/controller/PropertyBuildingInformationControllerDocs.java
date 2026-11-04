package com.homes.backend.domain.property.building.controller;

import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "매물 건물정보 API", description = "공공데이터로 매물의 건물정보를 수집하고 조회합니다.")
public interface PropertyBuildingInformationControllerDocs {
    /**
     * Documents the endpoint that returns a property's stored building information.
     */
    @Operation(summary = "매물 건물정보 조회", description = "자동 수집 상태와 저장된 건축물대장·K-apt 정보를 조회합니다. 상태는 NOT_COLLECTED, PENDING, PROCESSING, RESOLVED, PARTIAL, FAILED 중 하나입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 매물",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class)))
    })
    ApiResponse<BuildingInformationRespDto> get(
            @Parameter(description = "매물 ID", example = "1", required = true) @PathVariable Long propertyId
    );

    /**
     * Documents the public endpoint that collects and refreshes building information.
     */
    @Operation(summary = "매물 건물정보 수동 수집·갱신", description = "자동 수집 실패 후 재시도를 요청합니다. 요청은 자동 수집과 같은 작업 큐에 등록되고 동일 매물의 중복 요청은 하나로 합쳐집니다. 현재 수집 상태를 즉시 반환하며 누구나 실행할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수집 또는 갱신 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 매물",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "주소 해석 실패 또는 해당 주소의 건물정보 없음",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "건축물대장 제공기관 연결 실패",
                    content = @Content(schema = @Schema(implementation = com.homes.backend.global.response.ApiResponse.class)))
    })
    ApiResponse<BuildingInformationRespDto> resolve(
            @Parameter(description = "매물 ID", example = "1", required = true) @PathVariable Long propertyId
    );
}
