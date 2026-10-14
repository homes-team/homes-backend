package com.homes.backend.domain.user.controller;

import com.homes.backend.domain.user.dto.request.UserReportCreateReqDto;
import com.homes.backend.domain.user.service.UserReportService;
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
@RequestMapping("/users")
public class UserReportController implements UserReportControllerDocs {
    private final UserReportService userReportService;

    @Override
    @PostMapping("/{userId}/reports")
    public ApiResponse<Void> reportUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("userId") Long userId,
            @RequestBody @Valid UserReportCreateReqDto reqDto
    ) {
        if (userPrincipal == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        userReportService.createReport(userPrincipal.getId(), userId, reqDto);
        return ApiResponse.onSuccess(null);
    }
}
