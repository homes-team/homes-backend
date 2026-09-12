package com.homes.backend.domain.property.building.repository;

import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyBuildingInformationRepository extends JpaRepository<PropertyBuildingInformation, Long> {
}
