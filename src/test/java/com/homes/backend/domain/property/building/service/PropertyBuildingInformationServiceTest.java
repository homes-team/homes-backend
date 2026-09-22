package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.client.*;
import com.homes.backend.domain.property.building.entity.BuildingInformationStatus;
import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.property.insight.repository.PropertyAiEvaluationRepository;
import com.homes.backend.domain.user.entity.User;
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
    @Mock User owner;

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
    void resolvesAndStoresBuildingRegisterInformation() {
        ResolvedAddress address = new ResolvedAddress("서울특별시 도봉구 방학동 123-4", 37.66, 127.04,
                "1132010600", "11320", "10600", "0", "0123", "0004", null);
        BuildingRegisterTitle title = new BuildingRegisterTitle("register-1", "테스트아파트", address.normalizedAddress(),
                null, LocalDate.of(2004, 6, 18), 720, null, 52.4, 18, 2, 10, 500);

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(property.getUser()).thenReturn(owner);
        when(owner.getId()).thenReturn(7L);
        when(property.getId()).thenReturn(1L);
        when(property.getAddress()).thenReturn(address.normalizedAddress());
        when(geocodingService.resolve(property.getAddress())).thenReturn(Optional.of(address));
        when(buildingRegisterClient.findTitle(address)).thenReturn(Optional.of(title));
        when(buildingRegisterClient.findRecap(address)).thenReturn(Optional.of(
                new BuildingRegisterRecap("recap-1", LocalDate.of(2004, 6, 18), 720, null, 8, 540)));
        when(apartmentComplexClient.findByAddress(address)).thenReturn(Optional.empty());
        when(informationRepository.findById(1L)).thenReturn(Optional.empty());
        when(informationRepository.save(any(PropertyBuildingInformation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resolve(1L, 7L);

        assertThat(response.buildingRegisterId()).isEqualTo("register-1");
        assertThat(response.buildingYear()).isEqualTo(2004);
        assertThat(response.householdCount()).isEqualTo(720);
        assertThat(response.buildingCount()).isEqualTo(8);
        assertThat(response.parkingCount()).isEqualTo(540);
        assertThat(response.status()).isEqualTo("RESOLVED");
        verify(evaluationRepository).deleteById(1L);
    }

    @Test
    void preparesCollectionWhileHoldingThePropertyLock() {
        when(propertyRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(property));
        when(property.getId()).thenReturn(1L);
        when(property.getAddress()).thenReturn("서울특별시 도봉구 방학동 275");
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
        when(property.getId()).thenReturn(1L);
        when(property.getAddress()).thenReturn(oldAddress);
        PropertyBuildingInformation information = new PropertyBuildingInformation(property);
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 22, 1, 0);
        String token = information.claim(startedAt);
        AutomaticCollectionAttempt attempt = new AutomaticCollectionAttempt(token, oldAddress);
        information.queue("서울특별시 도봉구 창동 1", startedAt.plusMinutes(1));

        when(geocodingService.resolve(oldAddress)).thenReturn(Optional.of(resolvedAddress));
        when(buildingRegisterClient.findTitle(resolvedAddress)).thenReturn(Optional.of(title));
        when(buildingRegisterClient.findRecap(resolvedAddress)).thenReturn(Optional.empty());
        when(apartmentComplexClient.findByAddress(resolvedAddress)).thenReturn(Optional.empty());
        when(informationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(information));

        boolean saved = service.collectAutomatically(1L, attempt);

        assertThat(saved).isFalse();
        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PENDING);
        assertThat(information.getBuildingRegisterId()).isNull();
        verify(evaluationRepository, never()).deleteById(1L);
    }
}
