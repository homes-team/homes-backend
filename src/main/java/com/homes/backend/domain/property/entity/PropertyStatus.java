package com.homes.backend.domain.property.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PropertyStatus {
    AVAILABLE("거래가능"),
    MATCHED("매칭완료"), // 수수료 협상 완료(집주인이 중개사를 선택한 상태)
    COMPLETED("거래완료"),
    DELETED("삭제됨"); // 소프트 삭제 - 신고/입찰 등 참조 데이터를 보존하기 위해 물리적으로 지우지 않고 상태만 변경

    private final String description;
}
