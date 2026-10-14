package com.homes.backend.domain.property.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeType {
    MONTHLY_RENT("월세"),
    JEONSE("전세"),
    SALE("매매");

    private final String description;
}
