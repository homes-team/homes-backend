package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.event.BuildingInformationCollectionRequestedEvent;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.mockito.Mockito.*;

class PropertyBuildingInformationAutoLoadServiceTest {

    @Test
    void retriesTemporaryProviderFailureAndEventuallyCompletes() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        AutomaticCollectionAttempt attempt = new AutomaticCollectionAttempt("token", "서울특별시 도봉구 방학동 275");
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(true);
        when(informationService.claimAutomaticCollection(21L))
                .thenReturn(Optional.of(attempt), Optional.empty());
        when(informationService.renewAutomaticAttempt(21L, attempt)).thenReturn(true);
        doThrow(new CustomException(PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE))
                .doThrow(new CustomException(PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE))
                .doReturn(true)
                .when(informationService).collectAutomatically(21L, attempt);
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService, Runnable::run);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService, times(2)).renewAutomaticAttempt(21L, attempt);
        verify(informationService, times(3)).collectAutomatically(21L, attempt);
        verify(informationService, never()).markAutomaticFailure(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void recordsNonRetryableFailureWithoutAdditionalAttempt() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        AutomaticCollectionAttempt attempt = new AutomaticCollectionAttempt("token", "서울특별시 도봉구 방학동 275");
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(true);
        when(informationService.claimAutomaticCollection(21L))
                .thenReturn(Optional.of(attempt), Optional.empty());
        doThrow(new CustomException(PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND))
                .when(informationService).collectAutomatically(21L, attempt);
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService, Runnable::run);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService, times(1)).collectAutomatically(21L, attempt);
        verify(informationService).markAutomaticFailure(
                21L,
                attempt,
                PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND.getCode(),
                PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND.getMessage()
        );
    }

    @Test
    void skipsCollectionWhenCurrentAddressIsAlreadyHandled() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(false);
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService, Runnable::run);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService, never()).claimAutomaticCollection(anyLong());
        verify(informationService, never()).collectAutomatically(anyLong(), any());
    }

    @Test
    void leavesPreparedCollectionQueuedWhenExecutorRejectsSubmission() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(true);
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("saturated");
        };
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService, rejectingExecutor);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService).prepareAutomaticCollection(21L);
        verify(informationService, never()).claimAutomaticCollection(anyLong());
    }
}
