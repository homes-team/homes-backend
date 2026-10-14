package com.homes.backend.domain.property.registry.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.registry.entity.RegistryRiskLevel;
import org.springframework.stereotype.Component;

/**
 * 실제 등기부등본 API가 없어(개인 개발자가 접근 가능한 공개 API 자체가 존재하지 않음) 임시로 쓰는 가짜 스캐너.
 * 매물 ID를 기준으로 결정적(deterministic)인 값을 만들어서, 같은 매물은 재조회해도 항상 같은 결과가 나오게 한다.
 * 실제 매물 대부분은 안전하다는 현실을 반영해 SAFE 비중을 높게 잡았다 (85%/10%/5%).
 */
@Component
public class MockRegistryRiskScanner implements RegistryRiskScanner {

    @Override
    public RegistryRiskScanResult scan(Property property) {
        int bucket = Math.floorMod(property.getId(), 100);

        if (bucket < 85) {
            return new RegistryRiskScanResult(0, 0, RegistryRiskLevel.SAFE, "저당권/가압류가 확인되지 않았습니다.");
        } else if (bucket < 95) {
            return new RegistryRiskScanResult(1, 0, RegistryRiskLevel.CAUTION, "저당권 1건이 확인되어 주의가 필요합니다.");
        } else {
            return new RegistryRiskScanResult(2, 1, RegistryRiskLevel.DANGER, "저당권 2건, 가압류 1건이 확인되어 위험도가 높습니다.");
        }
    }
}
