package com.homes.backend.domain.property.building.controller;

import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.domain.property.building.service.PropertyBuildingInformationService;
import com.homes.backend.domain.property.building.service.PropertyBuildingInformationAutoLoadService;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties/{propertyId}/building-information")
public class PropertyBuildingInformationController implements PropertyBuildingInformationControllerDocs {
    private final PropertyBuildingInformationService service;
    private final PropertyBuildingInformationAutoLoadService autoLoadService;

    /**
     * Returns the most recently collected building information for a property.
     */
    @Override
    @GetMapping
    public ApiResponse<BuildingInformationRespDto> get(@PathVariable Long propertyId) {
        return ApiResponse.onSuccess(service.get(propertyId));
    }

    /**
     * Queues a building-information refresh for a public property detail page.
     */
    @Override
    @PostMapping("/resolve")
    public ApiResponse<BuildingInformationRespDto> resolve(@PathVariable Long propertyId) {
        return ApiResponse.onSuccess(autoLoadService.requestCollection(propertyId));
    }
}
