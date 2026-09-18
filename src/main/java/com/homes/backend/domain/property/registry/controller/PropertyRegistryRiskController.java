package com.homes.backend.domain.property.registry.controller;

import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.registry.dto.response.PropertyRegistryRiskResDto;
import com.homes.backend.domain.property.registry.repository.PropertyRegistryRiskRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
public class PropertyRegistryRiskController implements PropertyRegistryRiskControllerDocs {

    private final PropertyRegistryRiskRepository registryRiskRepository;

    @Override
    @GetMapping("/{propertyId}/registry-risk")
    public ApiResponse<PropertyRegistryRiskResDto> getRegistryRisk(@PathVariable Long propertyId) {
        PropertyRegistryRiskResDto response = registryRiskRepository.findByPropertyId(propertyId)
                .map(PropertyRegistryRiskResDto::from)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.REGISTRY_RISK_NOT_FOUND));

        return ApiResponse.onSuccess(response);
    }
}
