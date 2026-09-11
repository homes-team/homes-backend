package com.homes.backend.domain.property.building.client;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDataDateParsingTest {
    /**
     * Verifies parsing of compact building-register approval dates.
     */
    @Test
    void parsesBuildingRegisterApprovalDate() {
        assertThat(BuildingRegisterClient.parseDate("20040618")).isEqualTo(LocalDate.of(2004, 6, 18));
        assertThat(BuildingRegisterClient.parseDate("invalid")).isNull();
    }

    /**
     * Verifies parsing of K-apt approval dates that contain separators.
     */
    @Test
    void parsesApartmentApprovalDateWithSeparators() {
        assertThat(ApartmentBasisClient.parseDate("2004-06-18")).isEqualTo(LocalDate.of(2004, 6, 18));
        assertThat(ApartmentBasisClient.parseDate(null)).isNull();
    }
}
