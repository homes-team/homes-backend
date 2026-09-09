package com.homes.backend.domain.property.building.controller;

import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.domain.property.building.service.PropertyBuildingInformationService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties/{propertyId}/building-information")
public class PropertyBuildingInformationController implements PropertyBuildingInformationControllerDocs {
    private final PropertyBuildingInformationService service;

    @Override
    @GetMapping
    public ApiResponse<BuildingInformationRespDto> get(@PathVariable Long propertyId) {
        return ApiResponse.onSuccess(service.get(propertyId));
    }

    @Override
    @PostMapping("/resolve")
    public ApiResponse<BuildingInformationRespDto> resolve(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.onSuccess(service.resolve(propertyId, userPrincipal.getId()));
    }
}
