package com.homes.backend.global.security;

import lombok.Builder;

@Builder
public record TokenDto(
        String accessToken,
        String refreshToken,
        Long refreshTokenExpirationTime
) {}