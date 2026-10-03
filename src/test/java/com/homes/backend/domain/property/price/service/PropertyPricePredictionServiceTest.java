package com.homes.backend.domain.property.price.service;

import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.entity.TradeType;
import com.homes.backend.domain.property.price.client.ApartmentTradeProvider;
import com.homes.backend.domain.property.price.client.ApartmentTradeProviderException;
import com.homes.backend.domain.property.price.dto.PropertyPricePredictionRespDto;
import com.homes.backend.domain.property.price.model.ApartmentTrade;
import com.homes.backend.domain.property.price.model.PricePredictionPolicy;
import com.homes.backend.domain.property.price.model.PricePredictionStatus;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.geocoding.GeocodingService;
import com.homes.backend.global.geocoding.ResolvedAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyPricePredictionServiceTest {
    @Mock PropertyRepository propertyRepository;
    @Mock PropertyBuildingInformationRepository buildingInformationRepository;
    @Mock GeocodingService geocodingService;
    @Mock ApartmentTradeProvider apartmentTradeProvider;

    private PropertyPricePredictionService service;

    @BeforeEach
    void setUp() {
        service = new PropertyPricePredictionService(propertyRepository, buildingInformationRepository,
                geocodingService, apartmentTradeProvider, new PricePredictionPolicy());
    }

    @Test
    void returnsUnsupportedWithoutCallingProvidersForNonApartmentSale() {
        Property property = property(PropertyType.VILLA, TradeType.SALE);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));

        PropertyPricePredictionRespDto result = service.predict(21L);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.UNSUPPORTED);
        verify(geocodingService, never()).resolve(anyString());
        verify(apartmentTradeProvider, never()).findMonthlyTrades(anyString(), any());
    }

    @Test
    void predictsApartmentSaleUsingResolvedDistrictCode() {
        Property property = property(PropertyType.APARTMENT, TradeType.SALE);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));
        when(buildingInformationRepository.findById(21L)).thenReturn(Optional.empty());
        when(geocodingService.resolve(property.getAddress())).thenReturn(Optional.of(resolvedAddress()));
        AtomicInteger calls = new AtomicInteger();
        when(apartmentTradeProvider.findMonthlyTrades(anyString(), any())).thenAnswer(invocation ->
                calls.getAndIncrement() == 0 ? List.of(
                        trade(60_000, 1), trade(61_000, 2), trade(62_000, 3)) : List.of());

        PropertyPricePredictionRespDto result = service.predict(21L);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.AVAILABLE);
        assertThat(result.sampleCount()).isEqualTo(3);
        assertThat(result.predictedPrice()).isNotNull();
        verify(apartmentTradeProvider, atLeastOnce()).findMonthlyTrades(anyString(), any());
    }

    @Test
    void stopsAfterThreeConsecutiveProviderFailures() {
        Property property = property(PropertyType.APARTMENT, TradeType.SALE);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));
        when(buildingInformationRepository.findById(21L)).thenReturn(Optional.empty());
        when(geocodingService.resolve(property.getAddress())).thenReturn(Optional.of(resolvedAddress()));
        when(apartmentTradeProvider.findMonthlyTrades(anyString(), any()))
                .thenThrow(new ApartmentTradeProviderException("down"));

        PropertyPricePredictionRespDto result = service.predict(21L);

        assertThat(result.status()).isEqualTo(PricePredictionStatus.PROVIDER_UNAVAILABLE);
        verify(apartmentTradeProvider, org.mockito.Mockito.times(3)).findMonthlyTrades(anyString(), any());
    }

    private Property property(PropertyType propertyType, TradeType tradeType) {
        Property property = Property.builder()
                .title("테스트 매물").description("설명").address("서울 강남구 역삼동 123")
                .detailAddress("101동 1001호").tradeType(tradeType).propertyType(propertyType)
                .deposit(60_000L).monthlyRent(0L).maintenanceFee(20L).totalFloors(20).currentFloor(10)
                .area(84.0).desiredBrokerageFee(100.0).status(PropertyStatus.AVAILABLE).build();
        ReflectionTestUtils.setField(property, "id", 21L);
        return property;
    }

    private ResolvedAddress resolvedAddress() {
        return new ResolvedAddress("서울 강남구 역삼동 123", 37.5, 127.0,
                "1168010100", "11680", "10100", "0", "0123", "0000", "홈즈아파트");
    }

    private ApartmentTrade trade(long price, int monthsAgo) {
        return new ApartmentTrade("홈즈아파트", "역삼동", "123", 84.0, 10, 2015,
                price, LocalDate.now().minusMonths(monthsAgo));
    }
}
