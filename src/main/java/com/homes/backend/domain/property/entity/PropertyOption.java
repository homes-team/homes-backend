package com.homes.backend.domain.property.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyOption {
    ELEVATOR("엘리베이터"),
    SECURITY_GUARD("경비원"),
    PARKING("주차 가능"),
    BED("침대"),
    DESK("책상"),
    AIR_CONDITIONER("에어컨"),
    REFRIGERATOR("냉장고"),
    WASHING_MACHINE("세탁기"),
    MICROWAVE("전자레인지"),
    INDUCTION("인덕션 레인지"),
    GAS_STOVE("가스레인지"),
    SHOE_RACK("신발장"),
    CLOSET("옷장"),
    SINK("싱크대"),
    VERANDA("베란다"),
    FULL_OPTION("풀옵션");

    private final String description;
}
