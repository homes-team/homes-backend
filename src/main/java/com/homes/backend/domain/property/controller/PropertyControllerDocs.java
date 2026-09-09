package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.request.PropertyCreateReqDto;
import com.homes.backend.domain.property.dto.request.PropertyMapSearchReqDto;
import com.homes.backend.domain.property.dto.request.PropertyUpdateReqDto;
import com.homes.backend.domain.property.dto.response.PropertyDetailRespDto;
import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "매물(Property) API", description = "매물 등록, 상세 조회, 삭제를 담당하는 API")
public interface PropertyControllerDocs {
    @Operation(summary = "매물 등록", description = "매물 정보(JSON)를 등록합니다. 사진은 GET /properties/presigned-url로 미리 S3에 업로드한 뒤, " +
            "그 결과 URL들을 imageUrls에 담아 함께 보냅니다.")
    ApiResponse<Long> createProperty(
            @RequestBody PropertyCreateReqDto reqDto,
            @Parameter(hidden=true)@AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "전체 매물 리스트 조회", description = "카드형 UI에 들어갈 매물들의 간단한 정보 리스트를 조회합니다.")
    ApiResponse<List<PropertyListRespDto>> getAllProperties();

    @Operation(summary = "매물 상세 조회", description = "매물 ID를 통해 특정 매물의 상세 정보를 조회합니다.")
    ApiResponse<PropertyDetailRespDto> getProperty(
            @PathVariable Long propertyId,
            @Parameter(hidden=true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "매물 삭제", description = "매물 ID를 통해 특정 매물을 삭제합니다.")
    ApiResponse<Void> deleteProperty(
            @PathVariable Long propertyId,
            @Parameter(hidden=true)@AuthenticationPrincipal UserPrincipal userPrincipal);

    @Operation(summary = "매물 수정", description = "매물 정보(JSON)를 수정합니다. newImageUrls를 보내면 기존 사진 전체를 그걸로 교체합니다 " +
            "(사진도 presigned URL로 미리 업로드 후 URL만 전달).")
    ApiResponse<Void> updateProperty(
            @PathVariable Long propertyId,
            @RequestBody PropertyUpdateReqDto reqDto,
            @Parameter(hidden=true)@AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "지도 기반 매물 검색", description = "지도의 보이는 영역(좌표) 내에 있는 매물들을 필터링하여 조회합니다.")
    ApiResponse<List<PropertyListRespDto>> searchMapProperties(
            @Valid @ParameterObject @ModelAttribute PropertyMapSearchReqDto reqDto,
            @Parameter(hidden=true) @AuthenticationPrincipal UserPrincipal userPrincipal
    );

    @Operation(summary = "매물 랭킹 조회", description = "최근 가장 조회수와 찜 개수가 높은 상위 10개의 매물 리스트를 실시간으로 조회합니다.")
    ApiResponse<List<PropertyListRespDto>> getSurgeRankings();
}
