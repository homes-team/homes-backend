package com.homes.backend.domain.user.dto.response;

import com.homes.backend.domain.user.entity.User;

public record UserProfileResDto(
        Long userId,
        String email,
        String name,
        String nickname,
        String phone,
        String usagePurpose,
        boolean isIdentityVerified,
        String role
) {
    public static UserProfileResDto from(User user) {
        return new UserProfileResDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getPhone(),
                user.getUsagePurpose(),
                user.isIdentityVerified(),
                user.getRole()
        );
    }
}
