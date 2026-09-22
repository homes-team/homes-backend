package com.homes.backend.domain.property.building.entity;

import com.homes.backend.domain.property.entity.Property;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyBuildingInformationTest {

    @Test
    void treatsAssignedSharedPrimaryKeyAsNewUntilPersisted() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(21L);

        PropertyBuildingInformation information = new PropertyBuildingInformation(property);

        assertThat(information.getId()).isEqualTo(21L);
        assertThat(information.isNew()).isTrue();

        information.markNotNew();

        assertThat(information.isNew()).isFalse();
    }

    @Test
    void tracksAutomaticCollectionAttemptsAndFailure() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(21L);
        when(property.getAddress()).thenReturn("서울특별시 도봉구 방학동 275");
        PropertyBuildingInformation information = new PropertyBuildingInformation(property);

        assertThat(information.queue(property.getAddress())).isTrue();
        String token = information.claim(LocalDateTime.now());

        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PROCESSING);
        assertThat(information.getRetryCount()).isEqualTo(1);
        assertThat(information.getLastAttemptAt()).isNotNull();
        assertThat(token).isNotBlank();

        information.fail("BUILDING502_1", "공공 API 연결 실패");

        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.FAILED);
        assertThat(information.getLastErrorCode()).isEqualTo("BUILDING502_1");
        assertThat(information.getLastErrorMessage()).isEqualTo("공공 API 연결 실패");
    }

    @Test
    void recoversExpiredProcessingButRejectsAnActiveAttempt() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(21L);
        when(property.getAddress()).thenReturn("서울특별시 도봉구 방학동 275");
        PropertyBuildingInformation information = new PropertyBuildingInformation(property);
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 22, 1, 0);

        information.claim(startedAt);

        assertThat(information.queue(property.getAddress(), startedAt.plusMinutes(5))).isFalse();
        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PROCESSING);

        assertThat(information.queue(property.getAddress(), startedAt.plusMinutes(11))).isTrue();
        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PENDING);
    }

    @Test
    void retainsNewAddressAndRequeuesAfterInFlightAttempt() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(21L);
        when(property.getAddress()).thenReturn("old address");
        PropertyBuildingInformation information = new PropertyBuildingInformation(property);
        LocalDateTime startedAt = LocalDateTime.of(2026, 9, 22, 1, 0);
        String token = information.claim(startedAt);

        assertThat(information.queue("new address", startedAt.plusMinutes(1))).isFalse();
        assertThat(information.getRequestedAddress()).isEqualTo("new address");

        information.requeueIfSuperseded(token, "old address");

        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PENDING);
        assertThat(information.getRequestedAddress()).isEqualTo("new address");
    }
}
