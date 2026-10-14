package com.homes.backend.domain.infrastructure.controller;

import com.homes.backend.domain.infrastructure.dto.response.InfrastructureResDto;
import com.homes.backend.domain.infrastructure.entity.InfraType;
import com.homes.backend.domain.infrastructure.service.InfrastructureService;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties/infrastructure")
public class InfrastructureController implements InfrastructureControllerDocs {

    private final InfrastructureService infrastructureService;

    @Override
    @GetMapping
    public ApiResponse<List<InfrastructureResDto>> getInfrastructures(
            @RequestParam Double minLat,
            @RequestParam Double minLon,
            @RequestParam Double maxLat,
            @RequestParam Double maxLon,
            @RequestParam(required = false) InfraType infraType
    ) {
        List<InfrastructureResDto> response = infrastructureService.getInfrastructuresInBoundingBox(minLat, minLon, maxLat, maxLon, infraType);
        return ApiResponse.onSuccess(response);
    }
}
