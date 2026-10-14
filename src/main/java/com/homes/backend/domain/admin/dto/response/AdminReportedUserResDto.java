package com.homes.backend.domain.admin.dto.response;

import com.homes.backend.domain.user.entity.User;

public record AdminReportedUserResDto(
        Long userId,
        String email,
        String name,
        int reportCount,
        boolean isSuspicious
) {
    public static AdminReportedUserResDto from(User user) {
        return new AdminReportedUserResDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getReportCount(),
                user.isSuspicious()
        );
    }
}
