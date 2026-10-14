package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.client.*;
import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.insight.repository.PropertyAiEvaluationRepository;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.geocoding.GeocodingService;
import com.homes.backend.global.geocoding.ResolvedAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.homes.backend.domain.property.building.entity.PropertyBuildingInformation.PROCESSING_TIMEOUT;

@Service
@RequiredArgsConstructor
public class PropertyBuildingInformationService {
    private final PropertyRepository propertyRepository;
    private final PropertyBuildingInformationRepository informationRepository;
    private final GeocodingService geocodingService;
    private final BuildingRegisterClient buildingRegisterClient;
    private final ApartmentComplexClient apartmentComplexClient;
    private final ApartmentBasisClient apartmentBasisClient;
    private final PropertyAiEvaluationRepository evaluationRepository;

    /**
     * Returns stored building information or the explicit not-collected response.
     */
    @Transactional(readOnly = true)
    public BuildingInformationRespDto get(Long propertyId) {
        ensurePropertyExists(propertyId);
        return informationRepository.findById(propertyId)
                .map(BuildingInformationRespDto::from)
                .orElseGet(() -> BuildingInformationRespDto.notCollected(propertyId));
    }

    /**
     * Collects and persists building information when requested by the property owner.
     */
    @Transactional
    public BuildingInformationRespDto resolve(Long propertyId, Long userId) {
        Property property = ensurePropertyExists(propertyId);
        if (!property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.UNAUTHORIZED_ACCESS);
        }

        return BuildingInformationRespDto.from(collect(property, loadCollection(property.getAddress())));
    }

    @Transactional
    public boolean prepareAutomaticCollection(Long propertyId) {
        Property property = propertyRepository.findByIdWithPessimisticLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        PropertyBuildingInformation information = informationRepository.findByIdWithLock(propertyId)
                .orElseGet(() -> new PropertyBuildingInformation(property));
        boolean queued = information.queue(property.getAddress());
        if (information.isNew()) {
            informationRepository.save(information);
        }
        return queued;
    }

    @Transactional
    public Optional<AutomaticCollectionAttempt> claimAutomaticCollection(Long propertyId) {
        PropertyBuildingInformation information = informationRepository.findByIdWithLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        String token = information.claim(LocalDateTime.now());
        return token == null
                ? Optional.empty()
                : Optional.of(new AutomaticCollectionAttempt(token, information.getRequestedAddress()));
    }

    @Transactional
    public boolean renewAutomaticAttempt(Long propertyId, AutomaticCollectionAttempt attempt) {
        return informationRepository.findByIdWithLock(propertyId)
                .map(information -> information.renewAttempt(
                        attempt.token(), attempt.address(), LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public boolean collectAutomatically(Long propertyId, AutomaticCollectionAttempt attempt) {
        CollectionData collection = loadCollection(attempt.address());
        PropertyBuildingInformation information = informationRepository.findByIdWithLock(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        if (!information.isCurrentAttempt(attempt.token(), attempt.address())) {
            information.requeueIfSuperseded(attempt.token(), attempt.address());
            return false;
        }

        Integer previousBuildingYear = information.getBuildingYear();
        refresh(information, collection);
        if (!Objects.equals(previousBuildingYear, information.getBuildingYear())) {
            evaluationRepository.deleteById(propertyId);
        }
        return true;
    }

    @Transactional
    public void markAutomaticFailure(Long propertyId, AutomaticCollectionAttempt attempt,
                                     String errorCode, String errorMessage) {
        informationRepository.findByIdWithLock(propertyId).ifPresent(information -> {
            if (information.isCurrentAttempt(attempt.token(), attempt.address())) {
                information.fail(errorCode, errorMessage);
            } else {
                information.requeueIfSuperseded(attempt.token(), attempt.address());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Long> findRecoverablePropertyIds(int limit) {
        return informationRepository.findRecoverablePropertyIds(
                LocalDateTime.now().minus(PROCESSING_TIMEOUT),
                PageRequest.of(0, limit));
    }

    private CollectionData loadCollection(String requestedAddress) {
        ResolvedAddress address = geocodingService.resolve(requestedAddress)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.BUILDING_ADDRESS_RESOLUTION_FAILED));
        BuildingRegisterTitle register = buildingRegisterClient.findTitle(address)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND));
        BuildingRegisterRecap recap = buildingRegisterClient.findRecap(address).orElse(null);
        ApartmentComplex complex = apartmentComplexClient.findByAddress(address).orElse(null);
        ApartmentBasicInformation apartment = complex == null
                ? null
                : apartmentBasisClient.findBasicInformation(complex.kaptCode()).orElse(null);
        return new CollectionData(address, register, recap, complex, apartment);
    }

    private PropertyBuildingInformation collect(Property property, CollectionData collection) {
        PropertyBuildingInformation information = informationRepository.findById(property.getId())
                .orElseGet(() -> new PropertyBuildingInformation(property));
        Integer previousBuildingYear = information.getBuildingYear();
        refresh(information, collection);
        PropertyBuildingInformation saved = informationRepository.save(information);
        if (!Objects.equals(previousBuildingYear, saved.getBuildingYear())) {
            evaluationRepository.deleteById(property.getId());
        }
        return saved;
    }

    private void refresh(PropertyBuildingInformation information, CollectionData collection) {
        information.refresh(collection.address(), collection.register(), collection.recap(),
                collection.complex(), collection.apartment());
    }

    /**
     * Loads the requested property or raises the domain-specific not-found error.
     */
    private Property ensurePropertyExists(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
    }

    private record CollectionData(
            ResolvedAddress address,
            BuildingRegisterTitle register,
            BuildingRegisterRecap recap,
            ApartmentComplex complex,
            ApartmentBasicInformation apartment
    ) {
    }
}
