package com.homes.backend.global.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertySeedInitializerTest {
    @Mock PropertyRepository propertyRepository;
    @Mock UserRepository userRepository;

    private PropertySeedProperties properties;
    private PropertySeedInitializer initializer;
    private User owner;

    @BeforeEach
    void setUp() {
        properties = new PropertySeedProperties();
        properties.setOwnerEmail("seed-owner@homes.test");
        properties.setImageBaseUrl("http://localhost:8080/seed-images/");
        initializer = new PropertySeedInitializer(propertyRepository, userRepository, properties, new ObjectMapper());
        owner = User.builder()
                .id(77L)
                .email("seed-owner@homes.test")
                .nickname("seed-owner")
                .isIdentityVerified(true)
                .build();
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
    }

    @Test
    void createsNationwideDatasetWithExpectedTypeDistributionAndReusableImages() throws Exception {
        when(propertyRepository.existsByUserIdAndAddressAndDetailAddress(any(), any(), any())).thenReturn(false);

        initializer.run(mock(ApplicationArguments.class));

        ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository, times(164)).save(captor.capture());
        List<Property> seeded = captor.getAllValues();

        assertThat(seeded).hasSize(164);
        assertThat(seeded).filteredOn(property -> property.getPropertyType() == PropertyType.ONE_ROOM).hasSize(27);
        assertThat(seeded).filteredOn(property -> property.getPropertyType() == PropertyType.TWO_ROOM).hasSize(29);
        assertThat(seeded).filteredOn(property -> property.getPropertyType() == PropertyType.OFFICETEL).hasSize(29);
        assertThat(seeded).filteredOn(property -> property.getPropertyType() == PropertyType.APARTMENT).hasSize(79);
        assertThat(seeded).allSatisfy(property -> {
            assertThat(property.getTitle()).startsWith("[시드]");
            assertThat(property.getCoordinate().getSRID()).isEqualTo(4326);
        });
        assertThat(seeded).filteredOn(property -> property.getImages().isEmpty()).hasSize(13);
        assertThat(seeded).anySatisfy(property -> {
            assertThat(property.getImages()).isNotEmpty();
            assertThat(property.getImages().getFirst().getImageUrl())
                    .startsWith("http://localhost:8080/seed-images/");
            assertThat(property.getImages().getFirst().getIsThumbnail()).isTrue();
        });
    }

    @Test
    void skipsEveryListingWhenDatasetAlreadyExists() throws Exception {
        when(propertyRepository.existsByUserIdAndAddressAndDetailAddress(any(), any(), any())).thenReturn(true);

        initializer.run(mock(ApplicationArguments.class));

        verify(propertyRepository, never()).save(any());
        verify(propertyRepository, times(164))
                .existsByUserIdAndAddressAndDetailAddress(any(), any(), any());
    }
}
