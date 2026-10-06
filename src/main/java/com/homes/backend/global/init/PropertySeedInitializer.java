package com.homes.backend.global.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyDirection;
import com.homes.backend.domain.property.entity.PropertyImage;
import com.homes.backend.domain.property.entity.PropertyOption;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.entity.PropertyType;
import com.homes.backend.domain.property.entity.TradeType;
import com.homes.backend.domain.property.repository.PropertyRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
@ConditionalOnProperty(prefix = "app.seed.properties", name = "enabled", havingValue = "true")
public class PropertySeedInitializer implements ApplicationRunner {
    private static final String DATASET_PATH = "seed/property-regions-v1.json";
    private static final String SEED_MARKER = "[개발용 시드 #114]";

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertySeedProperties properties;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        String ownerEmail = properties.getOwnerEmail();
        if (!StringUtils.hasText(ownerEmail)) {
            throw new IllegalStateException("PROPERTY_SEED_OWNER_EMAIL is required when property seeding is enabled.");
        }

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalStateException("Property seed owner does not exist: " + ownerEmail));
        if (!owner.isIdentityVerified()) {
            throw new IllegalStateException("Property seed owner must be identity verified: " + ownerEmail);
        }

        SeedDataset dataset = readDataset();
        int created = 0;
        int skipped = 0;
        int sequence = 0;

        for (int regionIndex = 0; regionIndex < dataset.regions().size(); regionIndex++) {
            SeedRegion region = dataset.regions().get(regionIndex);
            List<SeedTemplate> templates = templates(region.scope(), regionIndex);
            for (int templateIndex = 0; templateIndex < templates.size(); templateIndex++) {
                SeedTemplate template = templates.get(templateIndex);
                String detailAddress = template.detailAddress();
                if (propertyRepository.existsByUserIdAndAddressAndDetailAddress(
                        owner.getId(), region.address(), detailAddress)) {
                    skipped++;
                    sequence++;
                    continue;
                }

                Property property = buildProperty(owner, region, template, regionIndex, templateIndex);
                addImages(property, template.propertyType(), sequence);
                propertyRepository.save(property);
                created++;
                sequence++;
            }
        }

        log.info("Property seed completed: version={}, created={}, skipped={}, total={}",
                dataset.version(), created, skipped, created + skipped);
    }

    private SeedDataset readDataset() throws Exception {
        try (InputStream inputStream = new ClassPathResource(DATASET_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, SeedDataset.class);
        }
    }

    private Property buildProperty(User owner, SeedRegion region, SeedTemplate template,
                                   int regionIndex, int templateIndex) {
        double offset = (templateIndex - 2) * 0.00035;
        Point coordinate = geometryFactory.createPoint(
                new Coordinate(region.longitude() + offset, region.latitude() - offset));
        double factor = region.priceFactor();

        Long deposit = Math.max(100L, Math.round(template.deposit() * factor));
        Long monthlyRent = Math.max(0L, Math.round(template.monthlyRent() * factor));
        String displayName = template.propertyType() == PropertyType.APARTMENT
                ? region.apartmentName() + " " + template.label()
                : region.district() + " " + region.dong() + " " + template.label();

        return Property.builder()
                .user(owner)
                .title("[시드] " + displayName)
                .description(SEED_MARKER + " 지도·검색·필터·클러스터링 확인을 위한 테스트 매물입니다.")
                .address(region.address())
                .detailAddress(template.detailAddress())
                .tradeType(template.tradeType())
                .propertyType(template.propertyType())
                .deposit(deposit)
                .monthlyRent(monthlyRent)
                .maintenanceFee(template.maintenanceFee())
                .totalFloors(template.totalFloors())
                .currentFloor(template.currentFloor())
                .direction(PropertyDirection.values()[(regionIndex + templateIndex) % 8])
                .remodelingYear((regionIndex + templateIndex) % 4 == 0 ? 2021 + (templateIndex % 4) : null)
                .area(template.area())
                .aiScore(65 + ((regionIndex * 7 + templateIndex * 5) % 31))
                .coordinate(coordinate)
                .desiredBrokerageFee(0.3 + ((regionIndex + templateIndex) % 3) * 0.05)
                .options(new ArrayList<>(template.options()))
                .nearestStation(null)
                .walkingTime(null)
                .status(PropertyStatus.AVAILABLE)
                .build();
    }

    private void addImages(Property property, PropertyType propertyType, int sequence) {
        String baseUrl = trimTrailingSlash(properties.getImageBaseUrl());
        if (!StringUtils.hasText(baseUrl) || sequence % 13 == 0) {
            return;
        }

        List<String> imageNames = switch (propertyType) {
            case ONE_ROOM -> List.of("one-room.png", "kitchen-bathroom.png");
            case TWO_ROOM -> List.of("two-room.png", "kitchen-bathroom.png");
            case OFFICETEL -> List.of("officetel.png", "kitchen-bathroom.png");
            case APARTMENT -> List.of("apartment-exterior.png", "apartment-interior.png", "kitchen-bathroom.png");
            default -> List.of("apartment-interior.png", "kitchen-bathroom.png");
        };

        for (int index = 0; index < imageNames.size(); index++) {
            property.getImages().add(PropertyImage.builder()
                    .property(property)
                    .imageUrl(baseUrl + "/" + imageNames.get(index))
                    .isThumbnail(index == 0)
                    .build());
        }
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.replaceFirst("/+$", "");
    }

    private static List<SeedTemplate> templates(String scope, int regionIndex) {
        if ("SEOUL".equals(scope)) {
            return List.of(
                    monthly(PropertyType.ONE_ROOM, "채광 좋은 원룸", "201호", 1_000, 55, 6, 19.8, 2, 5),
                    jeonse(PropertyType.TWO_ROOM, "분리형 투룸", "302호", 18_000, 8, 39.6, 3, 5),
                    monthly(PropertyType.OFFICETEL, "역세권 오피스텔", "805호", 2_000, 78, 10, 28.4, 8, 16),
                    saleApartment("59㎡형", "101동 503호", 55_000, 59.8, 5, 20),
                    saleApartment("84㎡형", "102동 1204호", 72_000, 84.9, 12, 25),
                    saleApartment("114㎡형", "103동 1802호", 95_000, 114.2, 18, 30)
            );
        }
        if ("GYEONGGI".equals(scope)) {
            return List.of(
                    monthly(PropertyType.TWO_ROOM, "실속형 투룸", "301호", 1_500, 60, 7, 41.2, 3, 6),
                    jeonse(PropertyType.OFFICETEL, "교통 편리한 오피스텔", "1007호", 22_000, 12, 31.5, 10, 18),
                    saleApartment("84㎡형", "105동 1403호", 68_000, 84.9, 14, 25)
            );
        }

        return switch (regionIndex % 4) {
            case 0 -> List.of(saleApartment("84㎡형", "101동 903호", 46_000, 84.9, 9, 20));
            case 1 -> List.of(monthly(PropertyType.OFFICETEL, "도심형 오피스텔", "707호", 1_500, 60, 9, 29.1, 7, 14));
            case 2 -> List.of(jeonse(PropertyType.TWO_ROOM, "생활권 좋은 투룸", "302호", 14_000, 6, 42.0, 3, 5));
            default -> List.of(monthly(PropertyType.ONE_ROOM, "깔끔한 원룸", "203호", 800, 45, 5, 21.0, 2, 4));
        };
    }

    private static SeedTemplate monthly(PropertyType type, String label, String detailAddress,
                                        long deposit, long rent, long maintenance,
                                        double area, int currentFloor, int totalFloors) {
        return new SeedTemplate(type, TradeType.MONTHLY_RENT, label, detailAddress, deposit, rent,
                maintenance, area, currentFloor, totalFloors,
                List.of(PropertyOption.FULL_OPTION, PropertyOption.AIR_CONDITIONER,
                        PropertyOption.REFRIGERATOR, PropertyOption.WASHING_MACHINE));
    }

    private static SeedTemplate jeonse(PropertyType type, String label, String detailAddress,
                                       long deposit, long maintenance,
                                       double area, int currentFloor, int totalFloors) {
        return new SeedTemplate(type, TradeType.JEONSE, label, detailAddress, deposit, 0,
                maintenance, area, currentFloor, totalFloors,
                List.of(PropertyOption.PARKING, PropertyOption.AIR_CONDITIONER,
                        PropertyOption.SHOE_RACK, PropertyOption.VERANDA));
    }

    private static SeedTemplate saleApartment(String label, String detailAddress,
                                              long price, double area,
                                              int currentFloor, int totalFloors) {
        return new SeedTemplate(PropertyType.APARTMENT, TradeType.SALE, label, detailAddress, price, 0,
                18, area, currentFloor, totalFloors,
                List.of(PropertyOption.ELEVATOR, PropertyOption.SECURITY_GUARD,
                        PropertyOption.PARKING, PropertyOption.VERANDA));
    }

    record SeedDataset(String version, List<SeedRegion> regions) {
    }

    record SeedRegion(String scope, String city, String district, String dong, String address,
                      double latitude, double longitude, double priceFactor, String apartmentName) {
    }

    record SeedTemplate(PropertyType propertyType, TradeType tradeType, String label, String detailAddress,
                        long deposit, long monthlyRent, long maintenanceFee, double area,
                        int currentFloor, int totalFloors, List<PropertyOption> options) {
    }
}
