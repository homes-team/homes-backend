package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.request.PropertyCreateReqDto;
import com.homes.backend.domain.property.dto.request.PropertyMapSearchReqDto;
import com.homes.backend.domain.property.dto.request.PropertyUpdateReqDto;
import com.homes.backend.domain.property.dto.response.PropertyDetailRespDto;
import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.domain.property.service.PropertyRankingService;
import com.homes.backend.domain.property.service.PropertyService;
import com.homes.backend.domain.property.service.RecentViewService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties")
public class PropertyController implements PropertyControllerDocs {
    private final PropertyService propertyService;
    private final RecentViewService recentViewService;
    private final PropertyRankingService propertyRankingService;

    @Override
    @PostMapping
    public ApiResponse<Long> createProperty(
            @RequestBody PropertyCreateReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long propertyId = propertyService.createProperty(reqDto, userPrincipal.getId());
        return ApiResponse.onSuccess(propertyId);
    }

    @Override
    @GetMapping
    public ApiResponse<List<PropertyListRespDto>> getAllProperties() {
        List<PropertyListRespDto> response = propertyService.getAllProperties();
        return ApiResponse.onSuccess(response);
    }

    @Override
    @GetMapping("/{propertyId}")
    public ApiResponse<PropertyDetailRespDto> getProperty(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        boolean isAdmin = userPrincipal != null && "ADMIN".equals(userPrincipal.getRole());
        PropertyDetailRespDto response = propertyService.getProperty(propertyId, isAdmin);

        // 최근 본 방 기록
        if (userPrincipal != null) { // 로그인한 유저라면
            try {
                recentViewService.addRecentView(userPrincipal.getId(), propertyId);
            } catch (Exception e) {
                // 최근 본 기록 실패는 응답에 영향 x, 넘어가기
            }
        }

        // 상세 조회 시 랭킹 점수 1점 증가
        try {
            propertyRankingService.incrementViewScore(propertyId);
        } catch (Exception e) {
            // Redis 장애가 메인 상세 조회를 막지 않도록 방어
        }

        return ApiResponse.onSuccess(response);
    }


    @Override
    @DeleteMapping("/{propertyId}")
    public ApiResponse<Void> deleteProperty(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        propertyService.deleteProperty(propertyId, userPrincipal.getId());
        return ApiResponse.onSuccess();
    }

    @Override
    @PatchMapping("/{propertyId}")
    public ApiResponse<Void> updateProperty(
            @PathVariable Long propertyId,
            @RequestBody PropertyUpdateReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        propertyService.updateProperty(propertyId, reqDto, userPrincipal.getId());
        return ApiResponse.onSuccess();
    }

    @Override
    @GetMapping("/map")
    public ApiResponse<List<PropertyListRespDto>> searchMapProperties(
            @Valid @ModelAttribute PropertyMapSearchReqDto reqDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String role = (userPrincipal != null) ? userPrincipal.getRole() : null;
        Long userId = (userPrincipal != null) ? userPrincipal.getId() : null;

        List<PropertyListRespDto> response = propertyService.searchMapProperties(reqDto, role, userId);
        return ApiResponse.onSuccess(response);
    }

    @Override
    @GetMapping("/surge-rankings")
    public ApiResponse<List<PropertyListRespDto>> getSurgeRankings() {
        List<PropertyListRespDto> response = propertyRankingService.getSurgeRankings();
        return ApiResponse.onSuccess(response);
    }

}
