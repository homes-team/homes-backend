package com.homes.backend.domain.property.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyType {
    ONE_ROOM("원룸"),
    TWO_ROOM("투룸"),
    VILLA("빌라"),
    HOUSE("주택"),
    APARTMENT("아파트"),
    OFFICETEL("오피스텔"),
    PRESALE("분양");

    private final String description;
}
