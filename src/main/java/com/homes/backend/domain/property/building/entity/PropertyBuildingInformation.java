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
import org.springframework.data.domain.Persistable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "property_building_information")
public class PropertyBuildingInformation implements Persistable<Long> {
    public static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);

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
    private String requestedAddress;
    private Integer retryCount;
    private LocalDateTime lastAttemptAt;

    @Column(length = 36)
    private String processingToken;

    private String lastErrorCode;

    @Column(length = 500)
    private String lastErrorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BuildingInformationStatus status;

    private String dataSources;

    private LocalDateTime collectedAt;

    @Transient
    private boolean newEntity = true;

    /**
     * Associates a new building-information record with its property.
     */
    public PropertyBuildingInformation(Property property) {
        this.property = property;
        this.propertyId = property.getId();
        this.status = BuildingInformationStatus.PENDING;
        this.requestedAddress = property.getAddress();
        this.retryCount = 0;
    }

    public boolean queue(String address) {
        return queue(address, LocalDateTime.now());
    }

    public boolean queue(String address, LocalDateTime now) {
        if (status == BuildingInformationStatus.PROCESSING) {
            if (lastAttemptAt == null || lastAttemptAt.plus(PROCESSING_TIMEOUT).isBefore(now)) {
                resetPending(address);
                return true;
            }
            if (!Objects.equals(requestedAddress, address)) {
                requestedAddress = address;
                retryCount = 0;
                lastErrorCode = null;
                lastErrorMessage = null;
            }
            return false;
        }
        if (status == BuildingInformationStatus.PENDING && Objects.equals(requestedAddress, address)) {
            return newEntity;
        }
        if ((status == BuildingInformationStatus.RESOLVED || status == BuildingInformationStatus.PARTIAL)
                && Objects.equals(requestedAddress, address)) {
            return false;
        }
        resetPending(address);
        return true;
    }

    private void resetPending(String address) {
        requestedAddress = address;
        retryCount = 0;
        processingToken = null;
        lastErrorCode = null;
        lastErrorMessage = null;
        status = BuildingInformationStatus.PENDING;
    }

    public String claim(LocalDateTime now) {
        if (status == BuildingInformationStatus.PROCESSING
                && lastAttemptAt != null
                && !lastAttemptAt.plus(PROCESSING_TIMEOUT).isBefore(now)) {
            return null;
        }
        if (status == BuildingInformationStatus.PROCESSING) {
            status = BuildingInformationStatus.PENDING;
            processingToken = null;
        }
        if (status != BuildingInformationStatus.PENDING) {
            return null;
        }

        processingToken = UUID.randomUUID().toString();
        status = BuildingInformationStatus.PROCESSING;
        retryCount = retryCount == null ? 1 : retryCount + 1;
        lastAttemptAt = now;
        lastErrorCode = null;
        lastErrorMessage = null;
        return processingToken;
    }

    public boolean renewAttempt(String token, String address, LocalDateTime now) {
        if (!isCurrentAttempt(token, address)) {
            requeueIfSuperseded(token, address);
            return false;
        }
        retryCount = retryCount == null ? 1 : retryCount + 1;
        lastAttemptAt = now;
        return true;
    }

    public boolean isCurrentAttempt(String token, String address) {
        return status == BuildingInformationStatus.PROCESSING
                && Objects.equals(processingToken, token)
                && Objects.equals(requestedAddress, address);
    }

    public void requeueIfSuperseded(String token, String address) {
        if (status == BuildingInformationStatus.PROCESSING
                && Objects.equals(processingToken, token)
                && !Objects.equals(requestedAddress, address)) {
            status = BuildingInformationStatus.PENDING;
            processingToken = null;
        }
    }

    public void fail(String errorCode, String errorMessage) {
        status = BuildingInformationStatus.FAILED;
        processingToken = null;
        lastErrorCode = errorCode;
        lastErrorMessage = errorMessage;
    }

    /**
     * Exposes the property-shared primary key to Spring Data.
     */
    @Override
    public Long getId() {
        return propertyId;
    }

    /**
     * Ensures a new shared-primary-key record is inserted even though its ID is already assigned.
     */
    @Override
    public boolean isNew() {
        return newEntity;
    }

    /**
     * Marks persisted and loaded records as existing so subsequent saves update them.
     */
    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
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
        processingToken = null;
        dataSources = apartment == null ? "BUILDING_REGISTER" : "BUILDING_REGISTER,K_APT";
        collectedAt = LocalDateTime.now();
        lastErrorCode = null;
        lastErrorMessage = null;
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
