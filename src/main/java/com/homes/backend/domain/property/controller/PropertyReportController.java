package com.homes.backend.domain.property.controller;

import com.homes.backend.domain.property.dto.request.ReportCreateReqDto;
import com.homes.backend.domain.property.dto.response.PropertyReportSummaryResDto;
import com.homes.backend.domain.property.service.PropertyReportService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.response.ApiResponse;
import com.homes.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/properties")
public class PropertyReportController implements PropertyReportControllerDocs{
    private final PropertyReportService reportService;

    @Override
    @PostMapping("/{propertyId}/reports")
    public ApiResponse<Void> reportProperty(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("propertyId") Long propertyId,
            @RequestBody @Valid ReportCreateReqDto reqDto
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        reportService.createReport(userPrincipal.getId(), propertyId, reqDto);
        return ApiResponse.onSuccess(null);
    }

    @Override
    @GetMapping("/{propertyId}/reports")
    public ApiResponse<PropertyReportSummaryResDto> getReportSummary(@PathVariable Long propertyId) {
        PropertyReportSummaryResDto response = reportService.getReportSummary(propertyId);
        return ApiResponse.onSuccess(response);
    }
}
