package com.homes.backend.domain.property.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    FAKE_PROPERTY("존재하지 않는 허위 매물이에요"),
    SOLD_OUT("이미 거래가 완료된 매물이에요"),
    PRICE_MISMATCH("등록된 가격(보증금/월세)이 실제와 달라요"),
    INFO_MISMATCH("옵션, 위치 등 매물 정보가 실제와 달라요"),
    OTHER("기타 사유");

    private final String description;
}
