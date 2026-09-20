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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

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

        return BuildingInformationRespDto.from(collect(property));
    }

    @Transactional
    public boolean prepareAutomaticCollection(Long propertyId) {
        Property property = ensurePropertyExists(propertyId);
        PropertyBuildingInformation information = informationRepository.findById(propertyId)
                .orElseGet(() -> new PropertyBuildingInformation(property));
        boolean queued = information.queue(property.getAddress());
        if (queued) {
            informationRepository.save(information);
        }
        return queued;
    }

    @Transactional
    public void beginAutomaticAttempt(Long propertyId) {
        PropertyBuildingInformation information = informationRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        information.beginAttempt();
    }

    @Transactional
    public void collectAutomatically(Long propertyId) {
        collect(ensurePropertyExists(propertyId));
    }

    @Transactional
    public void markAutomaticFailure(Long propertyId, String errorCode, String errorMessage) {
        informationRepository.findById(propertyId)
                .ifPresent(information -> information.fail(errorCode, errorMessage));
    }

    private PropertyBuildingInformation collect(Property property) {
        ResolvedAddress address = geocodingService.resolve(property.getAddress())
                .orElseThrow(() -> new CustomException(PropertyErrorCode.BUILDING_ADDRESS_RESOLUTION_FAILED));
        BuildingRegisterTitle register = buildingRegisterClient.findTitle(address)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND));
        BuildingRegisterRecap recap = buildingRegisterClient.findRecap(address).orElse(null);
        ApartmentComplex complex = apartmentComplexClient.findByAddress(address).orElse(null);
        ApartmentBasicInformation apartment = complex == null
                ? null
                : apartmentBasisClient.findBasicInformation(complex.kaptCode()).orElse(null);

        PropertyBuildingInformation information = informationRepository.findById(property.getId())
                .orElseGet(() -> new PropertyBuildingInformation(property));
        Integer previousBuildingYear = information.getBuildingYear();
        information.refresh(address, register, recap, complex, apartment);
        PropertyBuildingInformation saved = informationRepository.save(information);
        if (!Objects.equals(previousBuildingYear, saved.getBuildingYear())) {
            evaluationRepository.deleteById(property.getId());
        }
        return saved;
    }

    /**
     * Loads the requested property or raises the domain-specific not-found error.
     */
    private Property ensurePropertyExists(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
    }
}
