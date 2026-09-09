package com.homes.backend.domain.property.service;

import com.homes.backend.domain.property.dto.request.PropertyCreateReqDto;
import com.homes.backend.domain.property.dto.request.PropertyMapSearchReqDto;
import com.homes.backend.domain.property.dto.request.PropertyUpdateReqDto;
import com.homes.backend.domain.property.dto.response.PropertyDetailRespDto;
import com.homes.backend.domain.property.dto.response.PropertyListRespDto;
import com.homes.backend.domain.property.entity.*;
import com.homes.backend.domain.property.exception.PropertyErrorCode;
import com.homes.backend.domain.property.repository.*;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.storage.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PropertyFavoriteRepository propertyFavoriteRepository;
    private final RecentViewRepository recentViewRepository;
    private final StationRepository stationRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * GPS 표준인 4326(WGS84) 기반으로 Point를 만들어주는 팩토리
     */
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 매물 등록 (Create)
     */
    @Transactional
    public Long createProperty(PropertyCreateReqDto reqDto, Long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()-> new CustomException(UserErrorCode.USER_NOT_FOUND));

        /*
         *  Double 위도/경도를 공간 데이터(Point)로 변환
         *  (Coordinate는 X(경도), Y(위도) 순서로 넣음)
        */
        Point point = geometryFactory.createPoint(new Coordinate(reqDto.longitude(), reqDto.latitude()));

        /*
         * 가장 가까운 지하철역 찾기
         */
        StationDistanceProjection nearestResult = stationRepository.findNearestStationWithDistance(point);
        String calcNearestStation = null;
        Integer calcWalkingTime = null;

        if (nearestResult != null) {
            calcNearestStation = nearestResult.getPoiName();
            Double distanceMeter = nearestResult.getDistance();

            if (distanceMeter != null) {
                // 도보 시간 계산 (성인 걸음 1분 = 80m 기준, 올림 처리)
                int calculatedTime = (int) Math.ceil(distanceMeter / 80.0);

                // 도보 20분 초과 시 무의미하므로 null 처리
                calcWalkingTime = (calculatedTime > 20) ? null : calculatedTime;
            }
        }

        /*
         *  자동 부제목 생성 로직 (예: "서울시 강남구 역삼동 123" -> "강남구 역삼동 원/투룸")
         */
        String generatedTitle = generateAutomatedTitle(reqDto.address(), reqDto.propertyType().getDescription());

        Property property = Property.builder()
                .user(user)
                .title(generatedTitle)
                .description(reqDto.description())
                .address(reqDto.address())
                .detailAddress(reqDto.detailAddress())
                .tradeType(reqDto.tradeType())
                .propertyType(reqDto.propertyType())
                .deposit(reqDto.deposit())
                .monthlyRent(reqDto.monthlyRent())
                .maintenanceFee(reqDto.maintenanceFee())
                .totalFloors(reqDto.totalFloors())
                .currentFloor(reqDto.currentFloor())
                .area(reqDto.area())
                .coordinate(point)
                .desiredBrokerageFee(reqDto.desiredBrokerageFee())
                .options(reqDto.options())
                .nearestStation(calcNearestStation)
                .walkingTime(calcWalkingTime)
                .aiScore(85) // 가짜 AI 점수를 넣어두고, 추후 AI 모델 연동 시 고도화 예정
                .status(PropertyStatus.AVAILABLE)
                .build();

        /*
         * 매물 사진 - 클라이언트가 presigned URL로 이미 S3에 올려서 URL만 넘어온다
         */
        if (reqDto.imageUrls() != null && !reqDto.imageUrls().isEmpty()) {
            List<String> imageUrls = reqDto.imageUrls();
            String uploaderIdentity = "user:" + userId;

            for (int i = 0; i < imageUrls.size(); i++) {
                s3PresignedUrlService.validateAndConsumeUploadedFile(imageUrls.get(i), uploaderIdentity);

                PropertyImage propertyImage = PropertyImage.builder()
                        .imageUrl(imageUrls.get(i))
                        .isThumbnail(i == 0)
                        .property(property)
                        .build();

                property.getImages().add(propertyImage);
            }
        }

        return propertyRepository.save(property).getId();
    }

    /**
     * 주소 기반 자동 부제목 조합기
     */
    private String generateAutomatedTitle(String fullAddress, String propertyDescription) {
        String[] addressParts = fullAddress.split(" ");
        /*
         * 카카오 주소 API 결과 : 보통 "시 구 동 ..." 순서로 배치
         * -> 구&동 가져오기
         */
        if (addressParts.length >= 3) {
            return addressParts[1] + " " + addressParts[2] + " " + propertyDescription; // 결과: "강남구 역삼동 원룸"
        }

        return fullAddress + " " + propertyDescription; // 만약 주소가 짧은 예외 케이스라면 전체 주소를 사용
    }

    /**
     * 전체 매물 리스트 조회 (Read)
     */
    @Transactional(readOnly = true)
    public List<PropertyListRespDto> getAllProperties() {
        return propertyRepository.findAllByStatusNotOrderByIdDesc(PropertyStatus.DELETED).stream()
                .map(PropertyListRespDto::from)
                .toList();
    }

    /**
     * 매물 상세 조회 (Read). 삭제된 매물은 관리자만 전체 정보를 볼 수 있고,
     * 그 외에는 "삭제된 매물입니다" 안내에 필요한 최소 정보만 내려간다.
     */
    @Transactional(readOnly = true)
    public PropertyDetailRespDto getProperty(Long propertyId, boolean isAdmin) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        if (property.getStatus() == PropertyStatus.DELETED && !isAdmin) {
            return PropertyDetailRespDto.deleted(property);
        }

        return PropertyDetailRespDto.from(property);
    }

    /**
     * 소유권 검증 (내 매물이 맞는지 확인하는 로직)
     */
    public void validateOwnership(Property property, Long userId) {
        if (!property.getUser().getId().equals(userId)) {
            throw new CustomException(PropertyErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    /**
     * 매물 삭제 (Delete)
     */
    @Transactional
    public void deleteProperty(Long propertyId, Long userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        validateOwnership(property, userId);
        property.markAsDeleted();
    }


    /**
     * 매물 수정 (Update)
     */
    @Transactional
    public void updateProperty(Long propertyId, PropertyUpdateReqDto reqDto, Long userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new CustomException(PropertyErrorCode.PROPERTY_NOT_FOUND));

        validateOwnership(property, userId);

        /*
         * 수정된 데이터에 맞춰 위경도 Point 변환 및 자동 부제목 재조립
         */
        Point point = geometryFactory.createPoint(new Coordinate(reqDto.longitude(), reqDto.latitude()));
        String updatedTitle = generateAutomatedTitle(reqDto.address(), reqDto.propertyType().getDescription());

        /*
         * 주소가 변경되었을 수 있으므로 가장 가까운 지하철역 다시 계산
          */
        StationDistanceProjection nearestResult = stationRepository.findNearestStationWithDistance(point);
        String calcNearestStation = null;
        Integer calcWalkingTime = null;

        if (nearestResult != null) {
            calcNearestStation = nearestResult.getPoiName();
            Double distanceMeter = nearestResult.getDistance();

            if (distanceMeter != null) {
                // 도보 시간 계산 (성인 걸음 1분 = 80m 기준, 올림 처리)
                int calculatedTime = (int) Math.ceil(distanceMeter / 80.0);

                // 도보 20분 초과 시 무의미하므로 null 처리
                calcWalkingTime = (calculatedTime > 20) ? null : calculatedTime;
            }
        }

        property.update(
                updatedTitle, reqDto.description(), reqDto.address(), reqDto.detailAddress(),
                reqDto.tradeType(), reqDto.propertyType(), reqDto.deposit(),
                reqDto.monthlyRent(), reqDto.maintenanceFee(), reqDto.totalFloors(),
                reqDto.currentFloor(), reqDto.area(), point,
                reqDto.desiredBrokerageFee(),
                reqDto.options(),
                calcNearestStation,
                calcWalkingTime
        );

        /*
         * 새 사진 URL이 오면 기존 사진 전체를 교체 - 클라이언트가 presigned URL로 이미 S3에 올려서 URL만 넘어온다
         */
        if (reqDto.newImageUrls() != null) {
            List<String> newImageUrls = reqDto.newImageUrls();
            String uploaderIdentity = "user:" + userId;

            property.getImages().clear();

            for (int i = 0; i < newImageUrls.size(); i++) {
                s3PresignedUrlService.validateAndConsumeUploadedFile(newImageUrls.get(i), uploaderIdentity);

                PropertyImage propertyImage = PropertyImage.builder()
                        .imageUrl(newImageUrls.get(i))
                        .isThumbnail(i == 0)
                        .property(property)
                        .build();

                property.getImages().add(propertyImage);
            }
        }

    }

    /**
     * 유저가 내놓았던 집 확인(마이페이지)
     */
    @Transactional(readOnly = true)
    public List<PropertyListRespDto> getMyProperties(Long userId){
        List<Property> myProperties = propertyRepository.findAllByUserIdAndStatusNot(userId, PropertyStatus.DELETED);

        return myProperties.stream()
                .map(PropertyListRespDto::from)
                .toList();
    }

    /**
     * 지도 영역 내 매물 검색 및 다중 필터링
     */
    @Transactional(readOnly = true)
    public List<PropertyListRespDto> searchMapProperties(PropertyMapSearchReqDto reqDto, String role, Long userId) {

        /*
         * Bounding Box 생성
         */
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(reqDto.swLng(), reqDto.swLat()), // 남서 (좌측 하단) - 시작점
                new Coordinate(reqDto.swLng(), reqDto.neLat()), // 북서 (좌측 상단)
                new Coordinate(reqDto.neLng(), reqDto.neLat()), // 북동 (우측 상단)
                new Coordinate(reqDto.neLng(), reqDto.swLat()), // 남동 (우측 하단)
                new Coordinate(reqDto.swLng(), reqDto.swLat()) // 남서 (좌측 하단) - 끝점
        };
        Polygon boundingBox = geometryFactory.createPolygon(coords);
        boundingBox.setSRID(4326); // 4236: GPS(WGS84) 표준 좌표계

        /*
         * 권한별 지도 매물 상태 노출 필터링
         */
        List<PropertyStatus> targetStatuses;
        if ("AGENT".equals(role)) { // 중개사: 입찰해야 하므로 거래가능 매물만 노출
            targetStatuses = List.of(PropertyStatus.AVAILABLE);
        } else if ("ADMIN".equals(role)) {
            targetStatuses = null;
        } else { // 일반 유저 & 비로그인 사용자: 방을 구해야 하므로 거래가능 + 매칭완료 노출
            targetStatuses = List.of(PropertyStatus.AVAILABLE, PropertyStatus.MATCHED);
        }

        /*
         * 추천순 정렬일 때만 최근 본 방 ID 리스트 조회
         */

        PropertySortType normalizedSortBy = (reqDto.sortBy() == null)
                ? PropertySortType.RECOMMENDED
                : reqDto.sortBy();

        List<Long> recentViewedIds = List.of();
        if (normalizedSortBy == PropertySortType.RECOMMENDED && userId != null) {
            recentViewedIds = recentViewRepository.findTop20ByUserIdAndPropertyStatusNotOrderByViewedAtDesc(userId, PropertyStatus.DELETED)
                    .stream()
                    .map(rv -> rv.getProperty().getId())
                    .toList();
        }

        /*
         * QueryDSL 리포지토리 호출
         */
        List<Property> properties = propertyRepository.findPropertiesByMapAndFilters(
                boundingBox,
                targetStatuses, // 권한별로 결정된 매물 상태 리스트 전달
                reqDto.tradeType(),
                reqDto.propertyType(),
                reqDto.minDeposit(),
                reqDto.maxDeposit(),
                reqDto.minMonthlyRent(),
                reqDto.maxMonthlyRent(),
                reqDto.minArea(),
                reqDto.maxArea(),
                reqDto.keyword(),
                reqDto.options(),
                normalizedSortBy,
                recentViewedIds
        );

        return properties.stream()
                .map(PropertyListRespDto::from)
                .toList();
    }

    /**
     * 찜한 매물 조회
     */
    @Transactional(readOnly = true)
    public List<PropertyListRespDto> getMyFavoriteProperties(Long userId) {
        List<PropertyFavorite> favorites = propertyFavoriteRepository.findAllByUserIdWithProperty(userId, PropertyStatus.DELETED);

        return favorites.stream()
                .map(favorite -> PropertyListRespDto.from(favorite.getProperty()))
                .toList();
    }
}
