package com.homes.backend.global.geocoding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingServiceTest {
    @Test
    void padsBuildingRegisterLotNumbers() {
        assertThat(GeocodingService.padLotNumber("123")).isEqualTo("0123");
        assertThat(GeocodingService.padLotNumber("4")).isEqualTo("0004");
        assertThat(GeocodingService.padLotNumber("")).isEqualTo("0000");
    }
}
