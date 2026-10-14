package com.homes.backend.domain.infrastructure.controller;

import com.homes.backend.domain.infrastructure.dto.response.InfrastructureResDto;
import com.homes.backend.domain.infrastructure.entity.InfraType;
import com.homes.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "주변 인프라(Infrastructure) API", description = "지도 영역 기반의 병원, 마트, 학교 등 인프라 마커 조회 API")
public interface InfrastructureControllerDocs {
    @Operation(summary = "지도 영역 내 인프라 마커 조회", description = "지도의 남서쪽(min)과 북동쪽(max) 좌표(Bounding Box) 내에 존재하는 모든 인프라 마커를 조회합니다.")
    ApiResponse<List<InfrastructureResDto>> getInfrastructures(
            @Parameter(description = "남서쪽 위도 (최소 위도)", example = "37.500") Double minLat,
            @Parameter(description = "남서쪽 경도 (최소 경도)", example = "127.000") Double minLon,
            @Parameter(description = "북동쪽 위도 (최대 위도)", example = "37.510") Double maxLat,
            @Parameter(description = "북동쪽 경도 (최대 경도)", example = "127.010") Double maxLon,
            @Parameter(description = "시설 종류 필터 (예: CULTURE, HOSPITAL, MART, SUBWAY, BUS, PARK, SCHOOL). 입력하지 않으면 전체 조회", required = false) InfraType infraType
    );
}
