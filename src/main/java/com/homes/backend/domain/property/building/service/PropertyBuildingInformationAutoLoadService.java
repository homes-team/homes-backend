package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.event.BuildingInformationCollectionRequestedEvent;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
public class PropertyBuildingInformationAutoLoadService {
    private static final int MAX_ATTEMPTS = 3;
    private static final int RECOVERY_BATCH_SIZE = 100;
    private static final long RETRY_DELAY_MILLIS = 500L;

    private final PropertyBuildingInformationService informationService;
    private final Executor taskExecutor;

    public PropertyBuildingInformationAutoLoadService(
            PropertyBuildingInformationService informationService,
            @Qualifier("buildingInformationTaskExecutor") Executor taskExecutor
    ) {
        this.informationService = informationService;
        this.taskExecutor = taskExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BuildingInformationCollectionRequestedEvent event) {
        Long propertyId = event.propertyId();
        if (informationService.prepareAutomaticCollection(propertyId)) {
            submit(propertyId);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverAfterStartup() {
        dispatchRecoverableCollections();
    }

    @Scheduled(
            fixedDelayString = "${building-information.recovery-delay-millis:30000}",
            initialDelayString = "${building-information.recovery-delay-millis:30000}"
    )
    public void recoverQueuedCollections() {
        dispatchRecoverableCollections();
    }

    private void dispatchRecoverableCollections() {
        informationService.findRecoverablePropertyIds(RECOVERY_BATCH_SIZE).forEach(this::submit);
    }

    private void submit(Long propertyId) {
        try {
            taskExecutor.execute(() -> process(propertyId));
        } catch (RejectedExecutionException exception) {
            log.warn("건물정보 자동 수집 실행 지연: propertyId={}", propertyId);
            // prepareAutomaticCollection already persisted PENDING; the recovery poller will resubmit it.
        }
    }

    private void process(Long propertyId) {
        while (true) {
            Optional<AutomaticCollectionAttempt> attempt = informationService.claimAutomaticCollection(propertyId);
            if (attempt.isEmpty()) {
                return;
            }
            collectWithRetry(propertyId, attempt.get());
        }
    }

    private void collectWithRetry(Long propertyId, AutomaticCollectionAttempt collectionAttempt) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1 && !informationService.renewAutomaticAttempt(propertyId, collectionAttempt)) {
                    return;
                }
                informationService.collectAutomatically(propertyId, collectionAttempt);
                return;
            } catch (CustomException exception) {
                boolean retryable = exception.getErrorCode() == PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    informationService.markAutomaticFailure(propertyId, collectionAttempt,
                            exception.getErrorCode().getCode(), exception.getMessage());
                    log.warn("건물정보 자동 수집 실패: propertyId={}, attempt={}, code={}",
                            propertyId, attempt, exception.getErrorCode().getCode());
                    return;
                }
                waitBeforeRetry(attempt);
            } catch (RuntimeException exception) {
                informationService.markAutomaticFailure(propertyId, collectionAttempt,
                        "COMMON500", "건물정보 자동 수집 중 오류가 발생했습니다.");
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
