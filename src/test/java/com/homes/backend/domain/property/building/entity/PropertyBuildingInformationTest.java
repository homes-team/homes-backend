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
}
