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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyPricePredictionServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock PropertyRepository propertyRepository;
    @Mock PropertyBuildingInformationRepository buildingInformationRepository;
    @Mock GeocodingService geocodingService;
    @Mock ApartmentTradeProvider apartmentTradeProvider;

    private PropertyPricePredictionService service;

    @BeforeEach
    void setUp() {
        service = new PropertyPricePredictionService(propertyRepository, buildingInformationRepository,
                geocodingService, apartmentTradeProvider, new PricePredictionPolicy(), redisTemplate);
    }

    @Test
    void returnsUnsupportedWithoutCallingProvidersForNonApartmentSale() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("PRICE_PREDICTION_RATE:192.0.2.1"))))
                .thenReturn(1L);
        Property property = property(PropertyType.VILLA, TradeType.SALE);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));

        PropertyPricePredictionRespDto result = service.predict(21L, "192.0.2.1");

        assertThat(result.status()).isEqualTo(PricePredictionStatus.UNSUPPORTED);
        verify(geocodingService, never()).resolve(anyString());
        verify(apartmentTradeProvider, never()).findMonthlyTrades(anyString(), any());
    }

    @Test
    void predictsApartmentSaleUsingResolvedDistrictCode() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("PRICE_PREDICTION_RATE:192.0.2.1"))))
                .thenReturn(1L);
        Property property = property(PropertyType.APARTMENT, TradeType.SALE);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));
        when(buildingInformationRepository.findById(21L)).thenReturn(Optional.empty());
        when(geocodingService.resolve(property.getAddress())).thenReturn(Optional.of(resolvedAddress()));
        AtomicInteger calls = new AtomicInteger();
        when(apartmentTradeProvider.findMonthlyTrades(anyString(), any())).thenAnswer(invocation ->
                calls.getAndIncrement() == 0 ? List.of(
                        trade(60_000, 1), trade(61_000, 2), trade(62_000, 3)) : List.of());

        PropertyPricePredictionRespDto result = service.predict(21L, "192.0.2.1");

        assertThat(result.status()).isEqualTo(PricePredictionStatus.AVAILABLE);
        assertThat(result.sampleCount()).isEqualTo(3);
        assertThat(result.predictedPrice()).isNotNull();
        verify(apartmentTradeProvider, atLeastOnce()).findMonthlyTrades(anyString(), any());
    }

    @Test
    void stopsAfterThreeConsecutiveProviderFailures() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("PRICE_PREDICTION_RATE:192.0.2.1"))))
                .thenReturn(1L);
        Property property = property(PropertyType.APARTMENT, TradeType.SALE);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(property));
        when(buildingInformationRepository.findById(21L)).thenReturn(Optional.empty());
        when(geocodingService.resolve(property.getAddress())).thenReturn(Optional.of(resolvedAddress()));
        when(apartmentTradeProvider.findMonthlyTrades(anyString(), any()))
                .thenThrow(new ApartmentTradeProviderException("down"));

        PropertyPricePredictionRespDto result = service.predict(21L, "192.0.2.1");

        assertThat(result.status()).isEqualTo(PricePredictionStatus.PROVIDER_UNAVAILABLE);
        verify(apartmentTradeProvider, org.mockito.Mockito.times(3)).findMonthlyTrades(anyString(), any());
    }

    @Test
    void rejectsRequestsAcrossPropertyIdsBeforeAnyLookup() {
        Property first = property(PropertyType.APARTMENT, TradeType.SALE);
        Property second = property(PropertyType.APARTMENT, TradeType.SALE);
        ReflectionTestUtils.setField(second, "id", 22L);
        when(propertyRepository.findById(21L)).thenReturn(Optional.of(first));
        when(propertyRepository.findById(22L)).thenReturn(Optional.of(second));
        when(geocodingService.resolve(first.getAddress())).thenReturn(Optional.of(resolvedAddress()));
        AtomicInteger requests = new AtomicInteger();
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("PRICE_PREDICTION_RATE:192.0.2.1"))))
                .thenAnswer(invocation -> (long) requests.incrementAndGet());

        for (int i = 0; i < 30; i++) {
            service.predict(i % 2 == 0 ? 21L : 22L, "192.0.2.1");
        }
        clearInvocations(propertyRepository, buildingInformationRepository, geocodingService, apartmentTradeProvider);

        assertThatThrownBy(() -> service.predict(23L, "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.TOO_MANY_REQUESTS));
        verifyNoInteractions(propertyRepository, buildingInformationRepository, geocodingService, apartmentTradeProvider);

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("PRICE_PREDICTION_RATE:192.0.2.2"))))
                .thenReturn(1L);
        service.predict(22L, "192.0.2.2");
        verify(geocodingService).resolve(second.getAddress());
    }

    @Test
    void doesNotCallProvidersWhenRedisReturnsNoCount() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("PRICE_PREDICTION_RATE:192.0.2.1"))))
                .thenReturn(null);

        assertThatThrownBy(() -> service.predict(21L, "192.0.2.1"))
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR));
        verifyNoInteractions(propertyRepository, buildingInformationRepository, geocodingService, apartmentTradeProvider);
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
