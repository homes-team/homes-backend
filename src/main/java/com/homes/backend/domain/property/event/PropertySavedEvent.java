package com.homes.backend.domain.property.event;

/**
 * 매물이 생성되거나 수정되어 저장 트랜잭션이 커밋된 직후 발행된다.
 * 규칙 기반 허위매물 자동탐지(PropertyRiskDetectionService)가 이 이벤트를 구독해 동작한다.
 */
public record PropertySavedEvent(Long propertyId) {
}
