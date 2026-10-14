package com.homes.backend.domain.user.dto.response;

import com.homes.backend.domain.user.entity.User;

public record IdentityVerificationResDto(
        Long userId,
        boolean isIdentityVerified,
        String name
) {
    public static IdentityVerificationResDto from(User user) {
        return new IdentityVerificationResDto(
                user.getId(),
                user.isIdentityVerified(),
                user.getName()
        );
    }
}
