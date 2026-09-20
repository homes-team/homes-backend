package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.event.BuildingInformationCollectionRequestedEvent;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyBuildingInformationAutoLoadService {
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 500L;

    private final PropertyBuildingInformationService informationService;
    private final Set<Long> inFlightPropertyIds = ConcurrentHashMap.newKeySet();

    @Async("buildingInformationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BuildingInformationCollectionRequestedEvent event) {
        Long propertyId = event.propertyId();
        if (!inFlightPropertyIds.add(propertyId)) {
            log.debug("건물정보 자동 수집 중복 요청 생략: propertyId={}", propertyId);
            return;
        }

        try {
            if (!informationService.prepareAutomaticCollection(propertyId)) {
                return;
            }
            collectWithRetry(propertyId);
        } finally {
            inFlightPropertyIds.remove(propertyId);
        }
    }

    private void collectWithRetry(Long propertyId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                informationService.beginAutomaticAttempt(propertyId);
                informationService.collectAutomatically(propertyId);
                return;
            } catch (CustomException exception) {
                boolean retryable = exception.getErrorCode() == PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    informationService.markAutomaticFailure(propertyId, exception.getErrorCode().getCode(), exception.getMessage());
                    log.warn("건물정보 자동 수집 실패: propertyId={}, attempt={}, code={}",
                            propertyId, attempt, exception.getErrorCode().getCode());
                    return;
                }
                waitBeforeRetry(attempt);
            } catch (RuntimeException exception) {
                informationService.markAutomaticFailure(propertyId, "COMMON500", "건물정보 자동 수집 중 오류가 발생했습니다.");
                log.error("건물정보 자동 수집 중 예기치 않은 오류: propertyId={}", propertyId, exception);
                return;
            }
        }
    }

    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
