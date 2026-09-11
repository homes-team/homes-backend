package com.homes.backend.domain.property.building.service;

import com.homes.backend.domain.property.building.client.*;
import com.homes.backend.domain.property.building.dto.BuildingInformationRespDto;
import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.geocoding.GeocodingService;
import com.homes.backend.global.geocoding.ResolvedAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyBuildingInformationService {
    private final PropertyRepository propertyRepository;
    private final PropertyBuildingInformationRepository informationRepository;
    private final GeocodingService geocodingService;
    private final BuildingRegisterClient buildingRegisterClient;
    private final ApartmentComplexClient apartmentComplexClient;
    private final ApartmentBasisClient apartmentBasisClient;

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

        ResolvedAddress address = geocodingService.resolve(property.getAddress())
                .orElseThrow(() -> new CustomException(PropertyErrorCode.BUILDING_ADDRESS_RESOLUTION_FAILED));
        BuildingRegisterTitle register = buildingRegisterClient.findTitle(address)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.BUILDING_INFORMATION_NOT_FOUND));
        BuildingRegisterRecap recap = buildingRegisterClient.findRecap(address).orElse(null);
        ApartmentComplex complex = apartmentComplexClient.findByAddress(address).orElse(null);
        ApartmentBasicInformation apartment = complex == null
                ? null
                : apartmentBasisClient.findBasicInformation(complex.kaptCode()).orElse(null);

        PropertyBuildingInformation information = informationRepository.findById(propertyId)
                .orElseGet(() -> new PropertyBuildingInformation(property));
        information.refresh(address, register, recap, complex, apartment);
        return BuildingInformationRespDto.from(informationRepository.save(information));
    }

    /**
     * Loads the requested property or raises the domain-specific not-found error.
     */
    private Property ensurePropertyExists(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
    }
}
