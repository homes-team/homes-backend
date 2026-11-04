package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.client.*;
import com.homes.backend.domain.property.building.entity.BuildingInformationStatus;
import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.property.insight.repository.PropertyAiEvaluationRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.geocoding.GeocodingService;
import com.homes.backend.global.geocoding.ResolvedAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyBuildingInformationServiceTest {
    @Mock PropertyRepository propertyRepository;
    @Mock PropertyBuildingInformationRepository informationRepository;
    @Mock GeocodingService geocodingService;
    @Mock BuildingRegisterClient buildingRegisterClient;
    @Mock ApartmentComplexClient apartmentComplexClient;
    @Mock ApartmentBasisClient apartmentBasisClient;
    @Mock PropertyAiEvaluationRepository evaluationRepository;
    @Mock Property property;

    private PropertyBuildingInformationService service;

    /**
     * Creates the service with its mocked enrichment collaborators.
     */
    @BeforeEach
    void setUp() {
        service = new PropertyBuildingInformationService(propertyRepository, informationRepository, geocodingService,
                buildingRegisterClient, apartmentComplexClient, apartmentBasisClient, evaluationRepository);
    }

    /**
     * Verifies that resolved building-register data is persisted and returned.
     */
    @Test
    void collectsAndStoresBuildingRegisterInformationForCurrentAttempt() {
        ResolvedAddress address = new ResolvedAddress("서울특별시 도봉구 방학동 123-4", 37.66, 127.04,
                "1132010600", "11320", "10600", "0", "0123", "0004", null);
        BuildingRegisterTitle title = new BuildingRegisterTitle("register-1", "테스트아파트", address.normalizedAddress(),
                null, LocalDate.of(2004, 6, 18), 720, null, 52.4, 18, 2, 10, 500);

        when(property.getId()).thenReturn(1L);
        when(property.getAddress()).thenReturn(address.normalizedAddress());
        when(property.getStatus()).thenReturn(PropertyStatus.AVAILABLE);
        PropertyBuildingInformation information = new PropertyBuildingInformation(property);
        AutomaticCollectionAttempt attempt = new AutomaticCollectionAttempt(
                information.claim(LocalDateTime.now()), address.normalizedAddress());
        when(propertyRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(property));
        when(informationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(information));
        when(geocodingService.resolve(address.normalizedAddress())).thenReturn(Optional.of(address));
        when(buildingRegisterClient.findTitle(address)).thenReturn(Optional.of(title));
        when(buildingRegisterClient.findRecap(address)).thenReturn(Optional.of(
                new BuildingRegisterRecap("recap-1", LocalDate.of(2004, 6, 18), 720, null, 8, 540)));
        when(apartmentComplexClient.findByAddress(address)).thenReturn(Optional.empty());
        assertThat(service.collectAutomatically(1L, attempt)).isTrue();

        assertThat(information.getBuildingRegisterId()).isEqualTo("register-1");
        assertThat(information.getBuildingYear()).isEqualTo(2004);
        assertThat(information.getHouseholdCount()).isEqualTo(720);
        assertThat(information.getBuildingCount()).isEqualTo(8);
        assertThat(information.getParkingCount()).isEqualTo(540);
        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.RESOLVED);
        verify(evaluationRepository).deleteById(1L);
    }

    @Test
    void preparesCollectionWhileHoldingThePropertyLock() {
        when(propertyRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(property));
        when(property.getId()).thenReturn(1L);
        when(property.getAddress()).thenReturn("서울특별시 도봉구 방학동 275");
        when(property.getStatus()).thenReturn(PropertyStatus.AVAILABLE);
        when(informationRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        boolean queued = service.prepareAutomaticCollection(1L);

        assertThat(queued).isTrue();
        verify(informationRepository).save(any(PropertyBuildingInformation.class));
    }

    @Test
    void discardsProviderResultWhenAddressChangedDuringCollection() {
        String oldAddress = "서울특별시 도봉구 방학동 275";
        ResolvedAddress resolvedAddress = new ResolvedAddress(oldAddress, 37.66, 127.04,
                "1132010600", "11320", "10600", "0", "0275", "0000", null);
        BuildingRegisterTitle title = new BuildingRegisterTitle("register-1", "테스트아파트", oldAddress,
                null, LocalDate.of(2004, 6, 18), 720, null, 52.4, 18, 2, 10, 500);
        String newAddress = "서울특별시 도봉구 창동 1";
        when(property.getId()).thenReturn(1L);
        when(property.getAddress()).thenReturn(oldAddress, newAddress);
        when(property.getStatus()).thenReturn(PropertyStatus.AVAILABLE);
        PropertyBuildingInformation information = new PropertyBuildingInformation(property);
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 22, 1, 0);
        String token = information.claim(startedAt);
        AutomaticCollectionAttempt attempt = new AutomaticCollectionAttempt(token, oldAddress);

        when(geocodingService.resolve(oldAddress)).thenReturn(Optional.of(resolvedAddress));
        when(buildingRegisterClient.findTitle(resolvedAddress)).thenReturn(Optional.of(title));
        when(buildingRegisterClient.findRecap(resolvedAddress)).thenReturn(Optional.empty());
        when(apartmentComplexClient.findByAddress(resolvedAddress)).thenReturn(Optional.empty());
        when(propertyRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(property));
        when(informationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(information));

        boolean saved = service.collectAutomatically(1L, attempt);

        assertThat(saved).isFalse();
        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PENDING);
        assertThat(information.getBuildingRegisterId()).isNull();
        verify(evaluationRepository, never()).deleteById(1L);
    }

    @Test
    void treatsDeletedPropertyAsNotFound() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(property.getStatus()).thenReturn(PropertyStatus.DELETED);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.get(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(PropertyErrorCode.PROPERTY_NOT_FOUND));

        verify(informationRepository, never()).findById(1L);
    }

    @Test
    void doesNotQueueDeletedPropertyForCollection() {
        when(propertyRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(property));
        when(property.getStatus()).thenReturn(PropertyStatus.DELETED);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.prepareAutomaticCollection(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(PropertyErrorCode.PROPERTY_NOT_FOUND));

        verify(informationRepository, never()).findByIdWithLock(1L);
    }
}
