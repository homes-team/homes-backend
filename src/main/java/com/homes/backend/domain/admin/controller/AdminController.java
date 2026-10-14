package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.response.AdminRealtorDetailResDto;
import com.homes.backend.domain.admin.dto.response.AdminRealtorSummaryResDto;
import com.homes.backend.domain.admin.service.AdminService;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/realtors")
@RequiredArgsConstructor
public class AdminController implements AdminControllerDocs {

    private final AdminService adminService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<AdminRealtorSummaryResDto>> getRealtors() {
        return ApiResponse.onSuccess(adminService.getRealtors());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{realtorId}")
    public ApiResponse<AdminRealtorDetailResDto> getRealtorDetail(@PathVariable Long realtorId) {
        return ApiResponse.onSuccess(adminService.getRealtorDetail(realtorId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{realtorId}/approve")
    public ApiResponse<Void> approveRealtor(@PathVariable Long realtorId) {
        adminService.approveRealtor(realtorId);
        return ApiResponse.onSuccess();
    }
}
