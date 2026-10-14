package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.request.AdminUserWithdrawReqDto;
import com.homes.backend.domain.admin.dto.response.AdminReportedUserResDto;
import com.homes.backend.domain.admin.dto.response.AdminUserReportDetailResDto;
import com.homes.backend.domain.admin.service.AdminUserService;
import com.homes.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserControllerDocs {

    private final AdminUserService adminUserService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    public ApiResponse<List<AdminReportedUserResDto>> getReportedUsers() {
        return ApiResponse.onSuccess(adminUserService.getReportedUsers());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/reports")
    public ApiResponse<List<AdminUserReportDetailResDto>> getUserReportDetail(@PathVariable Long userId) {
        return ApiResponse.onSuccess(adminUserService.getUserReportDetail(userId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> withdrawUser(@PathVariable Long userId, @RequestBody @Valid AdminUserWithdrawReqDto request) {
        adminUserService.withdrawUser(userId, request.reason());
        return ApiResponse.onSuccess();
    }
}
