package com.homes.backend.domain.property.building.entity;

import com.homes.backend.domain.property.entity.Property;
import org.junit.jupiter.api.Test;

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
        information.beginAttempt();

        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.PROCESSING);
        assertThat(information.getRetryCount()).isEqualTo(1);
        assertThat(information.getLastAttemptAt()).isNotNull();

        information.fail("BUILDING502_1", "공공 API 연결 실패");

        assertThat(information.getStatus()).isEqualTo(BuildingInformationStatus.FAILED);
        assertThat(information.getLastErrorCode()).isEqualTo("BUILDING502_1");
        assertThat(information.getLastErrorMessage()).isEqualTo("공공 API 연결 실패");
    }
}
