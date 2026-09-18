package com.homes.backend.domain.property.registry.service;

import com.homes.backend.domain.property.entity.Property;

/**
 * 매물의 등기부등본(권리관계) 위험도를 스캔하는 인터페이스.
 * 지금은 {@link MockRegistryRiskScanner}만 있지만, 실제 등기부등본 조회 API를 계약하게 되면
 * 이 인터페이스의 새 구현체(RealRegistryRiskScanner 등)로 교체하기만 하면 된다 -
 * 이벤트 리스너(PropertyRegistryRiskScanService)와 엔티티/리포지토리/컨트롤러는 전혀 손댈 필요가 없다.
 */
public interface RegistryRiskScanner {
    RegistryRiskScanResult scan(Property property);
}
