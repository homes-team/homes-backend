package com.homes.backend.domain.property.registry.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.event.PropertySavedEvent;
import com.homes.backend.domain.property.registry.entity.PropertyRegistryRisk;
import com.homes.backend.domain.property.registry.entity.RegistryRiskLevel;
import com.homes.backend.domain.property.registry.repository.PropertyRegistryRiskRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매물 등기부등본(권리관계) 위험도 스캔. PropertyRiskDetectionService(신고/패턴 기반 규칙)와는 별개의 리스너로,
 * 같은 PropertySavedEvent를 함께 구독한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyRegistryRiskScanService {

    private final PropertyRepository propertyRepository;
    private final PropertyRegistryRiskRepository registryRiskRepository;
    private final RegistryRiskScanner registryRiskScanner;

    // 매물 저장 트랜잭션이 실제로 커밋된 뒤에만 스캔한다. 여기서 예외가 나도 매물 저장 자체(원래 요청)에
    // 영향을 주면 안 되므로 반드시 흡수한다 (PropertyRiskDetectionService와 동일한 안전장치).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePropertySaved(PropertySavedEvent event) {
        try {
            Property property = propertyRepository.findById(event.propertyId()).orElse(null);
            if (property == null || property.getStatus() == PropertyStatus.DELETED) {
                return;
            }

            RegistryRiskScanResult result = registryRiskScanner.scan(property);
            boolean alreadyScanned = registryRiskRepository.findByPropertyId(property.getId()).isPresent();

            // markSuspicious()는 alreadyScanned 분기의 벌크 UPDATE(clearAutomatically=true)보다 먼저 호출해야 한다.
            // 벌크 쿼리가 영속성 컨텍스트를 비워버리면 그 뒤에 이 managed 엔티티를 건드려도 detach된 상태라
            // dirty checking이 안 먹혀서 커밋 시점에 반영되지 않는다 (flushAutomatically=true가 이 변경사항을
            // 벌크 쿼리 실행 전에 먼저 flush해주므로, 순서만 지키면 안전하게 반영된다).
            if (result.riskLevel() == RegistryRiskLevel.DANGER) {
                log.info("등기부등본 위험도 DANGER 감지 - 의심 매물로 자동 전환: propertyId={}, mortgageCount={}, seizureCount={}",
                        property.getId(), result.mortgageCount(), result.seizureCount());
                property.markSuspicious(true);
            }

            if (alreadyScanned) {
                // 값이 이전과 완전히 같아도(결정적 mock이라 흔함) updated_at("최종 스캔일시")이 항상 갱신되도록 벌크 쿼리로 처리
                registryRiskRepository.updateScanResult(
                        property.getId(), result.mortgageCount(), result.seizureCount(), result.riskLevel(), result.summary());
            } else {
                registryRiskRepository.save(
                        PropertyRegistryRisk.builder()
                                .property(property)
                                .mortgageCount(result.mortgageCount())
                                .seizureCount(result.seizureCount())
                                .riskLevel(result.riskLevel())
                                .summary(result.summary())
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("등기부등본 위험도 스캔 중 오류 발생 (매물 저장 자체는 이미 성공, 스캔만 건너뜀): propertyId={}",
                    event.propertyId(), e);
        }
    }
}
