package com.homes.backend.domain.property.price.service;

import com.homes.backend.domain.property.building.entity.PropertyBuildingInformation;
import com.homes.backend.domain.property.building.repository.PropertyBuildingInformationRepository;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.entity.TradeType;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.price.client.ApartmentTradeProvider;
import com.homes.backend.domain.property.price.client.ApartmentTradeProviderException;
import com.homes.backend.domain.property.price.dto.PropertyPricePredictionRespDto;
import com.homes.backend.domain.property.price.model.ApartmentTrade;
import com.homes.backend.domain.property.price.model.PricePrediction;
import com.homes.backend.domain.property.price.model.PricePredictionPolicy;
import com.homes.backend.domain.property.price.model.PricePredictionStatus;
import com.homes.backend.domain.property.price.model.PricePredictionTarget;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.geocoding.GeocodingService;
import com.homes.backend.global.geocoding.ResolvedAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyPricePredictionService {
    private static final int LOOKBACK_MONTHS = 18;
    // Share the request budget across all property IDs and application instances.
    private static final int RATE_LIMIT_MAX_REQUESTS = 30;
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], 60)
            end
            return count
            """, Long.class);

    private final PropertyRepository propertyRepository;
    private final PropertyBuildingInformationRepository buildingInformationRepository;
    private final GeocodingService geocodingService;
    private final ApartmentTradeProvider apartmentTradeProvider;
    private final PricePredictionPolicy predictionPolicy;
    private final StringRedisTemplate redisTemplate;

    public PropertyPricePredictionRespDto predict(Long propertyId, String clientIp) {
        Long requestCount = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of("PRICE_PREDICTION_RATE:" + clientIp));
        if (requestCount == null) {
            throw new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (requestCount > RATE_LIMIT_MAX_REQUESTS) {
            throw new CustomException(GlobalErrorCode.TOO_MANY_REQUESTS);
        }

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));
        if (property.getPropertyType() != PropertyType.APARTMENT || property.getTradeType() != TradeType.SALE) {
            return response(property, PricePrediction.unavailable(PricePredictionStatus.UNSUPPORTED,
                    "현재는 아파트 매매 매물만 가격 예측을 지원합니다."));
        }

        Optional<PropertyBuildingInformation> buildingInformation = buildingInformationRepository.findById(propertyId);
        Optional<ResolvedAddress> resolvedAddress = geocodingService.resolve(property.getAddress());
        String sigunguCode = buildingInformation.map(PropertyBuildingInformation::getSigunguCode)
                .filter(StringUtils::hasText)
                .or(() -> resolvedAddress.map(ResolvedAddress::sigunguCode))
                .orElse(null);
        if (!StringUtils.hasText(sigunguCode)) {
            return response(property, PricePrediction.unavailable(PricePredictionStatus.ADDRESS_UNAVAILABLE,
                    "매물 주소에서 실거래 조회 지역을 확인할 수 없습니다."));
        }

        Integer buildingYear = buildingInformation.map(PropertyBuildingInformation::getBuildingYear).orElse(null);
        String apartmentName = resolvedAddress.map(ResolvedAddress::buildingName).orElse(null);
        String normalizedAddress = resolvedAddress.map(ResolvedAddress::normalizedAddress).orElse(property.getAddress());
        String legalDongName = legalDongName(normalizedAddress);
        String lotNumber = resolvedAddress.map(PropertyPricePredictionService::lotNumber)
                .orElseGet(() -> buildingInformation.map(PropertyPricePredictionService::lotNumber).orElse(null));
        PricePredictionTarget target = new PricePredictionTarget(
                property.getArea(), property.getCurrentFloor(), buildingYear, apartmentName, legalDongName, lotNumber);

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        List<ApartmentTrade> trades = new ArrayList<>();
        int failedMonths = 0;
        int consecutiveFailures = 0;
        for (int i = 0; i < LOOKBACK_MONTHS; i++) {
            try {
                trades.addAll(apartmentTradeProvider.findMonthlyTrades(sigunguCode, currentMonth.minusMonths(i)));
                consecutiveFailures = 0;
            } catch (ApartmentTradeProviderException exception) {
                failedMonths++;
                consecutiveFailures++;
                log.warn("실거래 월별 조회를 건너뜁니다: sigunguCode={}, month={}", sigunguCode, currentMonth.minusMonths(i));
                if (consecutiveFailures >= 3) break;
            }
        }
        if (trades.isEmpty() && failedMonths > 0) {
            return response(property, PricePrediction.unavailable(PricePredictionStatus.PROVIDER_UNAVAILABLE,
                    "실거래가 제공기관에 일시적으로 연결할 수 없습니다."));
        }
        return response(property, predictionPolicy.predict(target, trades, today));
    }

    private static PropertyPricePredictionRespDto response(Property property, PricePrediction prediction) {
        return PropertyPricePredictionRespDto.from(property.getId(), property.getTradeType(), prediction);
    }

    private static String legalDongName(String address) {
        if (!StringUtils.hasText(address)) return null;
        for (String token : address.split("\\s+")) {
            if (token.endsWith("동") || token.endsWith("읍") || token.endsWith("면")
                    || token.endsWith("리") || token.matches(".*\\d가$")) {
                return token;
            }
        }
        return null;
    }

    private static String lotNumber(ResolvedAddress address) {
        return lotNumber(address.landTypeCode(), address.mainLotNumber(), address.subLotNumber());
    }

    private static String lotNumber(PropertyBuildingInformation information) {
        return lotNumber(information.getLandTypeCode(), information.getMainLotNumber(), information.getSubLotNumber());
    }

    private static String lotNumber(String landTypeCode, String main, String sub) {
        if (!StringUtils.hasText(main)) return null;
        String normalizedMain = stripLeadingZeros(main);
        String normalizedSub = stripLeadingZeros(sub);
        String prefix = "1".equals(landTypeCode) ? "산" : "";
        return prefix + normalizedMain + ("0".equals(normalizedSub) ? "" : "-" + normalizedSub);
    }

    private static String stripLeadingZeros(String value) {
        if (!StringUtils.hasText(value)) return "0";
        return value.replaceFirst("^0+(?!$)", "");
    }
}
