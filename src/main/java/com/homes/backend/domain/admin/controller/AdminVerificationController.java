package com.homes.backend.domain.admin.controller;

import com.homes.backend.domain.admin.dto.request.AdminOwnerVerificationUpdateReqDto;
import com.homes.backend.domain.admin.dto.response.AdminOwnerVerificationListResDto;
import com.homes.backend.domain.admin.service.AdminVerificationService;
import com.homes.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/verifications")
@RequiredArgsConstructor
public class AdminVerificationController implements AdminVerificationControllerDocs {

    private final AdminVerificationService adminVerificationService;

    @GetMapping("/owner/manual-reviews")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ApiResponse<List<AdminOwnerVerificationListResDto>> getManualReviewList() {
        List<AdminOwnerVerificationListResDto> response = adminVerificationService.getManualReviewList();
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping("/owner/{verificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ApiResponse<Void> processManualReview(
            @PathVariable Long verificationId,
            @RequestBody AdminOwnerVerificationUpdateReqDto reqDto) {

        adminVerificationService.processManualReview(verificationId, reqDto);
        return ApiResponse.onSuccess();
    }
}
