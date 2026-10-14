package com.homes.backend.domain.user.dto.response;

import com.homes.backend.domain.user.entity.User;

public record UserUpdateProfileResDto(
        Long userId,
        String nickname,
        String usagePurpose
) {
    public static UserUpdateProfileResDto from(User user) {
        return new UserUpdateProfileResDto(
                user.getId(),
                user.getNickname(),
                user.getUsagePurpose()
        );
    }
}
