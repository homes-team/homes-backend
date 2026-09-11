package com.homes.backend.domain.property.building.entity;

import com.homes.backend.domain.property.building.client.ApartmentBasicInformation;
import com.homes.backend.domain.property.building.client.ApartmentComplex;
import com.homes.backend.domain.property.building.client.BuildingRegisterRecap;
import com.homes.backend.domain.property.building.client.BuildingRegisterTitle;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.global.geocoding.ResolvedAddress;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "property_building_information")
public class PropertyBuildingInformation {
    @Id
    @Column(name = "property_id")
    private Long propertyId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    private String normalizedAddress;
    private String legalDongCode;
    private String sigunguCode;
    private String bjdongCode;
    private String landTypeCode;
    private String mainLotNumber;
    private String subLotNumber;
    private String buildingRegisterId;
    private String kaptCode;
    private LocalDate approvalDate;
    private Integer buildingYear;
    private Integer householdCount;
    private Integer buildingCount;
    private Double buildingHeightMeters;
    private Integer groundFloorCount;
    private Integer undergroundFloorCount;
    private Integer elevatorCount;
    private Integer parkingCount;
    private String corridorType;
    private String heatingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildingInformationStatus status;

    @Column(nullable = false)
    private String dataSources;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    /**
     * Associates a new building-information record with its property.
     */
    public PropertyBuildingInformation(Property property) {
        this.property = property;
        this.propertyId = property.getId();
    }

    /**
     * Replaces enrichment fields with the latest building-register and K-apt data.
     */
    public void refresh(ResolvedAddress address, BuildingRegisterTitle register, BuildingRegisterRecap recap,
                        ApartmentComplex complex, ApartmentBasicInformation apartment) {
        normalizedAddress = address.normalizedAddress();
        legalDongCode = address.legalDongCode();
        sigunguCode = address.sigunguCode();
        bjdongCode = address.bjdongCode();
        landTypeCode = address.landTypeCode();
        mainLotNumber = address.mainLotNumber();
        subLotNumber = address.subLotNumber();

        buildingRegisterId = register == null ? null : register.registerId();
        kaptCode = complex == null ? null : complex.kaptCode();
        approvalDate = first(register == null ? null : register.approvalDate(),
                recap == null ? null : recap.approvalDate(), apartment == null ? null : apartment.approvalDate());
        buildingYear = approvalDate == null ? null : approvalDate.getYear();
        householdCount = first(recap == null ? null : recap.householdCount(),
                register == null ? null : register.householdCount(), apartment == null ? null : apartment.householdCount());
        buildingCount = first(recap == null ? null : recap.buildingCount(),
                apartment == null ? null : apartment.buildingCount());
        buildingHeightMeters = register == null ? null : register.heightMeters();
        groundFloorCount = first(register == null ? null : register.groundFloorCount(), apartment == null ? null : apartment.highestFloor());
        undergroundFloorCount = register == null ? null : register.undergroundFloorCount();
        elevatorCount = register == null ? null : register.elevatorCount();
        parkingCount = first(recap == null ? null : recap.parkingCount(),
                apartment == null ? null : apartment.parkingCount(), register == null ? null : register.parkingCount());
        corridorType = apartment == null ? null : apartment.corridorType();
        heatingType = apartment == null ? null : apartment.heatingType();
        status = buildingRegisterId != null && approvalDate != null ? BuildingInformationStatus.RESOLVED : BuildingInformationStatus.PARTIAL;
        dataSources = apartment == null ? "BUILDING_REGISTER" : "BUILDING_REGISTER,K_APT";
        collectedAt = LocalDateTime.now();
    }

    /**
     * Returns the first non-null value in source-priority order.
     */
    @SafeVarargs
    private static <T> T first(T... candidates) {
        for (T candidate : candidates) {
            if (candidate != null) return candidate;
        }
        return null;
    }
}
