package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.event.PropertySavedEvent;
import com.homes.backend.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 규칙 기반 허위매물 자동탐지. 매물이 생성/수정될 때마다 신고 없이도 의심스러운 패턴을 스스로 검사한다.
 * 현재 구현된 규칙: 같은 주소+상세주소(같은 호실)를 서로 다른 유저가 동시에 매물로 등록한 경우
 * (같은 집을 두 명 이상이 자기 집이라며 내놓는 것은 그 자체로 명백한 이상 신호이므로, 가격 비교 없이 즉시 의심 매물로 전환한다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyRiskDetectionService {

    private final PropertyRepository propertyRepository;

    // 매물 저장 트랜잭션이 실제로 커밋된 뒤에만 검사한다 (저장 자체가 실패/롤백되면 검사할 대상이 없음)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePropertySaved(PropertySavedEvent event) {
        Property property = propertyRepository.findById(event.propertyId()).orElse(null);
        if (property == null || property.getStatus() == PropertyStatus.DELETED) {
            return;
        }

        List<Property> conflictingListings = propertyRepository.findConflictingAddressListings(
                property.getAddress(),
                property.getDetailAddress(),
                property.getId(),
                property.getUser().getId(),
                PropertyStatus.DELETED
        );

        if (conflictingListings.isEmpty()) {
            return;
        }

        log.info("동일 매물 중복 등록 감지 - 의심 매물로 자동 전환: propertyId={}, 충돌 매물 수={}",
                property.getId(), conflictingListings.size());

        property.markSuspicious(true);
        conflictingListings.forEach(conflicting -> conflicting.markSuspicious(true));
    }
}
