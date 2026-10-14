package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.request.AdminPropertySuspiciousReqDto;
import com.homes.backend.domain.admin.dto.response.AdminPropertyReportDetailResDto;
import com.homes.backend.domain.admin.dto.response.AdminReportedPropertyResDto;
import com.homes.backend.domain.admin.service.AdminPropertyService;
import com.homes.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPropertyController implements AdminPropertyControllerDocs {

    private final AdminPropertyService adminPropertyService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public ApiResponse<List<AdminReportedPropertyResDto>> getReportedProperties() {
        return ApiResponse.onSuccess(adminPropertyService.getReportedProperties());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/suspicious-properties")
    public ApiResponse<List<AdminReportedPropertyResDto>> getSuspiciousProperties() {
        return ApiResponse.onSuccess(adminPropertyService.getSuspiciousProperties());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/properties/{propertyId}/reports")
    public ApiResponse<List<AdminPropertyReportDetailResDto>> getPropertyReportDetail(@PathVariable Long propertyId) {
        return ApiResponse.onSuccess(adminPropertyService.getPropertyReportDetail(propertyId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/properties/{propertyId}")
    public ApiResponse<Void> deleteProperty(@PathVariable Long propertyId) {
        adminPropertyService.deleteProperty(propertyId);
        return ApiResponse.onSuccess();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/properties/{propertyId}/suspicious")
    public ApiResponse<Void> updateSuspiciousStatus(
            @PathVariable Long propertyId,
            @RequestBody @Valid AdminPropertySuspiciousReqDto reqDto
    ) {
        adminPropertyService.updateSuspiciousStatus(propertyId, reqDto.isSuspicious());
        return ApiResponse.onSuccess();
    }
}
