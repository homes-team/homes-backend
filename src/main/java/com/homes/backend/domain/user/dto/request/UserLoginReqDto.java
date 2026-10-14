package com.homes.backend.domain.user.dto.request;

public record UserLoginReqDto(
        String email,
        String password
) {}