package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.event.BuildingInformationCollectionRequestedEvent;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PropertyBuildingInformationAutoLoadServiceTest {

    @Test
    void retriesTemporaryProviderFailureAndEventuallyCompletes() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(true);
        doThrow(new CustomException(PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE))
                .doThrow(new CustomException(PropertyErrorCode.BUILDING_PROVIDER_UNAVAILABLE))
                .doNothing()
                .when(informationService).collectAutomatically(21L);
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService, times(3)).beginAutomaticAttempt(21L);
        verify(informationService, times(3)).collectAutomatically(21L);
        verify(informationService, never()).markAutomaticFailure(anyLong(), anyString(), anyString());
    }

    @Test
    void recordsNonRetryableFailureWithoutAdditionalAttempt() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(true);
        doThrow(new CustomException(PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND))
                .when(informationService).collectAutomatically(21L);
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService, times(1)).beginAutomaticAttempt(21L);
        verify(informationService, times(1)).collectAutomatically(21L);
        verify(informationService).markAutomaticFailure(
                21L,
                PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND.getCode(),
                PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND.getMessage()
        );
    }

    @Test
    void skipsCollectionWhenCurrentAddressIsAlreadyHandled() {
        PropertyBuildingInformationService informationService = mock(PropertyBuildingInformationService.class);
        when(informationService.prepareAutomaticCollection(21L)).thenReturn(false);
        PropertyBuildingInformationAutoLoadService autoLoadService =
                new PropertyBuildingInformationAutoLoadService(informationService);

        autoLoadService.handle(new BuildingInformationCollectionRequestedEvent(21L));

        verify(informationService, never()).beginAutomaticAttempt(anyLong());
        verify(informationService, never()).collectAutomatically(anyLong());
    }
}
