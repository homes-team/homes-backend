package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.entity.TradeType;
import com.homes.backend.domain.property.event.PropertySavedEvent;
import com.homes.backend.domain.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 규칙 기반 허위매물 자동탐지. 매물이 생성/수정될 때마다 신고 없이도 의심스러운 패턴을 스스로 검사한다.
 * 신고 누적(5회) 자동 전환, 관리자 수동 지정과는 별개의 경로이며, 전부 같은 Property.isSuspicious 필드를 공유한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyRiskDetectionService {

    // 이 시간 안에 같은 유저가 이만큼 등록하면 스팸/매크로성 대량등록으로 간주 (실사용 데이터 없이 정한 보수적인 초기값 - 추후 조정 필요)
    private static final int BULK_REGISTRATION_THRESHOLD = 10;
    private static final Duration BULK_REGISTRATION_WINDOW = Duration.ofMinutes(10);

    private static final double MAX_REASONABLE_AREA = 1000.0; // ㎡ 기준 - 이보다 크면 입력 오류로 간주

    private final PropertyRepository propertyRepository;

    // 매물 저장 트랜잭션이 실제로 커밋된 뒤에만 검사한다 (저장 자체가 실패/롤백되면 검사할 대상이 없음)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePropertySaved(PropertySavedEvent event) {
        Property property = propertyRepository.findById(event.propertyId()).orElse(null);
        if (property == null || property.getStatus() == PropertyStatus.DELETED) {
            return;
        }

        boolean suspicious = false;
        suspicious |= checkConflictingAddress(property);
        suspicious |= checkInconsistentPricing(property);
        suspicious |= checkBulkRegistration(property);
        suspicious |= checkReRegistrationEvasion(property);

        if (suspicious) {
            property.markSuspicious(true);
        }
    }

    /**
     * 규칙 1: 같은 주소+상세주소(같은 호실)로 이미 살아있는 매물이 또 있는 경우 (소유자 동일 여부 무관).
     * 정상적인 재등록은 "삭제 후 재등록" 순서라 이 시점엔 옛 매물이 DELETED 상태이므로 걸리지 않는다 -
     * 삭제 없이 같은 호실이 중복으로 살아있는 것 자체가 이상 신호(중복 클릭 실수 또는 어뷰징)이므로 가격 비교 없이 즉시 전환한다.
     */
    private boolean checkConflictingAddress(Property property) {
        List<Property> conflictingListings = propertyRepository.findConflictingAddressListings(
                property.getAddress(),
                property.getDetailAddress(),
                property.getId(),
                PropertyStatus.DELETED
        );

        if (conflictingListings.isEmpty()) {
            return false;
        }

        log.info("동일 매물 중복 등록 감지 - 의심 매물로 자동 전환: propertyId={}, 충돌 매물 수={}",
                property.getId(), conflictingListings.size());

        conflictingListings.forEach(conflicting -> conflicting.markSuspicious(true));
        return true;
    }

    /**
     * 규칙 2: 거래유형-금액 조합이 논리적으로 모순되거나, 면적이 비정상적인 경우.
     * 시세 판단이 아니라 데이터 자체의 내적 일관성만 보므로 임계값 튜닝이 필요 없다.
     */
    private boolean checkInconsistentPricing(Property property) {
        boolean saleWithMonthlyRent = property.getTradeType() == TradeType.SALE && property.getMonthlyRent() > 0;
        boolean jeonseWithMonthlyRent = property.getTradeType() == TradeType.JEONSE && property.getMonthlyRent() > 0;
        boolean invalidArea = property.getArea() <= 0 || property.getArea() > MAX_REASONABLE_AREA;
        boolean bothZero = property.getDeposit() == 0 && property.getMonthlyRent() == 0;

        boolean inconsistent = saleWithMonthlyRent || jeonseWithMonthlyRent || invalidArea || bothZero;

        if (inconsistent) {
            log.info("가격/면적 논리 모순 감지 - 의심 매물로 자동 전환: propertyId={}, tradeType={}, deposit={}, monthlyRent={}, area={}",
                    property.getId(), property.getTradeType(), property.getDeposit(), property.getMonthlyRent(), property.getArea());
        }

        return inconsistent;
    }

    /**
     * 규칙 3: 같은 유저가 짧은 시간 안에 매물을 대량 등록하는 경우 (스팸/매크로성 의심).
     */
    private boolean checkBulkRegistration(Property property) {
        LocalDateTime windowStart = LocalDateTime.now().minus(BULK_REGISTRATION_WINDOW);
        long recentCount = propertyRepository.countByUserIdAndCreatedAtAfter(property.getUser().getId(), windowStart);

        boolean bulk = recentCount >= BULK_REGISTRATION_THRESHOLD;

        if (bulk) {
            log.info("단시간 대량등록 감지 - 의심 매물로 자동 전환: propertyId={}, userId={}, 최근 {}분 내 등록 수={}",
                    property.getId(), property.getUser().getId(), BULK_REGISTRATION_WINDOW.toMinutes(), recentCount);
        }

        return bulk;
    }

    /**
     * 규칙 4: 신고 이력이 있던(의심 매물이었거나 신고를 받았던) 삭제 매물을, 같은 유저가 같은 주소+호실로 재등록하는 경우.
     * 정상적인 재등록(거래 무산 후 다시 올리는 등)은 신고 이력이 없으므로 걸리지 않는다 - "신고 이력 세탁" 시도만 잡는다.
     */
    private boolean checkReRegistrationEvasion(Property property) {
        boolean isEvasion = propertyRepository.existsPreviouslyFlaggedDeletedListing(
                property.getUser().getId(),
                property.getAddress(),
                property.getDetailAddress(),
                property.getId(),
                PropertyStatus.DELETED
        );

        if (isEvasion) {
            log.info("신고 이력 세탁 시도(재등록) 감지 - 의심 매물로 자동 전환: propertyId={}, userId={}",
                    property.getId(), property.getUser().getId());
        }

        return isEvasion;
    }
}
