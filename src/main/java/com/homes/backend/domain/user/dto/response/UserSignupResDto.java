package com.homes.backend.domain.user.dto.response;
import com.homes.backend.domain.user.entity.User;

public record UserSignupResDto(
        Long userId,
        String email,
        String name
) {
    public static UserSignupResDto from(User user) {
        return new UserSignupResDto(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}