package com.homes.backend.global.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.storage.S3PresignedUrlService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertySeedInitializerTest {
    @Mock PropertyRepository propertyRepository;
    @Mock UserRepository userRepository;
    @Mock EntityManager entityManager;
    @Mock S3PresignedUrlService s3PresignedUrlService;

    private static final List<String> IMAGE_POOL = List.of(
            "https://test-bucket.s3.ap-southeast-2.amazonaws.com/seed-images/test-1.jpg",
            "https://test-bucket.s3.ap-southeast-2.amazonaws.com/seed-images/test-2.jpg",
            "https://test-bucket.s3.ap-southeast-2.amazonaws.com/seed-images/test-3.jpg"
    );

    private PropertySeedProperties properties;
    private PropertySeedInitializer initializer;
    private User owner;

    @BeforeEach
    void setUp() {
        properties = new PropertySeedProperties();
        properties.setOwnerEmail("seed-owner@homes.test");
        initializer = new PropertySeedInitializer(
                propertyRepository, userRepository, entityManager, properties, new ObjectMapper(), s3PresignedUrlService);
        owner = User.builder()
                .id(77L)
                .email("seed-owner@homes.test")
                .nickname("seed-owner")
                .isIdentityVerified(true)
                .build();
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(s3PresignedUrlService.listObjectUrls(anyString())).thenReturn(IMAGE_POOL);
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
            assertThat(property.getTitle()).isNotBlank();
            assertThat(property.getDescription()).isNotBlank();
            assertThat(property.getCoordinate().getSRID()).isEqualTo(4326);
        });
        assertThat(seeded).filteredOn(property -> property.getImages().isEmpty()).hasSize(13);
        assertThat(seeded).anySatisfy(property -> {
            assertThat(property.getImages()).isNotEmpty();
            assertThat(property.getImages().getFirst().getImageUrl()).isIn(IMAGE_POOL);
            assertThat(property.getImages().getFirst().getIsThumbnail()).isTrue();
        });
    }

    @Test
    void skipsEveryListingWhenDatasetAlreadyExists() throws Exception {
        when(propertyRepository.existsByUserIdAndAddressAndDetailAddress(any(), any(), any())).thenReturn(true);

        initializer.run(mock(ApplicationArguments.class));

        verify(propertyRepository, never()).save(any());
        InOrder inOrder = inOrder(entityManager, propertyRepository);
        inOrder.verify(entityManager).lock(owner, LockModeType.PESSIMISTIC_WRITE);
        inOrder.verify(propertyRepository, times(164))
                .existsByUserIdAndAddressAndDetailAddress(any(), any(), any());
    }
}
