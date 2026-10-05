package com.homes.backend.domain.property.price.controller;

import com.homes.backend.domain.property.price.dto.PropertyPricePredictionRespDto;
import com.homes.backend.domain.property.price.service.PropertyPricePredictionService;
import com.homes.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties")
public class PropertyPricePredictionController implements PropertyPricePredictionControllerDocs {
    private final PropertyPricePredictionService predictionService;

    @Override
    @GetMapping("/{propertyId}/price-prediction")
    public ApiResponse<PropertyPricePredictionRespDto> predict(@PathVariable Long propertyId, HttpServletRequest request) {
        return ApiResponse.onSuccess(predictionService.predict(propertyId, request.getRemoteAddr()));
    }
}
