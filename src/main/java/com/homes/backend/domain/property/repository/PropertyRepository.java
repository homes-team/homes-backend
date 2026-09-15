package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import jakarta.persistence.LockModeType;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, PropertyRepositoryCustom {
    /**
     * 내가 등록한 매물 목록. 삭제(DELETED)된 매물은 자연히 제외
     */
    List<Property> findAllByUserIdAndStatusNot(Long userId, PropertyStatus status);

    /**
     * 전체 매물 목록. 삭제(DELETED)된 매물은 자연히 제외
     */
    List<Property> findAllByStatusNotOrderByIdDesc(PropertyStatus status);

    /**
     * 주어진 좌표(중개사무소) 기준으로 거래가능한 매물을 거리순으로 조회 (DB 레벨 공간 연산 + LIMIT 적용)
     */
    @Query(value = "SELECT p.id AS id, p.address AS address, p.detail_address AS detailAddress, " +
            "p.trade_type AS tradeType, p.deposit AS deposit, p.monthly_rent AS monthlyRent, " +
            "p.desired_brokerage_fee AS desiredBrokerageFee, " +
            "ST_DistanceSphere(p.coordinate, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) AS distanceInMeters " +
            "FROM properties p " +
            "WHERE p.status = :#{#status.name()} " +
            "ORDER BY distanceInMeters ASC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<PropertyDistanceProjection> findByStatusOrderByDistance(
            @Param("status") PropertyStatus status,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("limit") int limit
    );

    /**
     * 입찰가능 매물 목록: 거래가능 매물 중, 해당 중개사가 아직 입찰을 넣지 않은 것만 거리순으로 조회
     */
    @Query(value = "SELECT p.id AS id, p.address AS address, p.detail_address AS detailAddress, " +
            "p.trade_type AS tradeType, p.deposit AS deposit, p.monthly_rent AS monthlyRent, " +
            "p.desired_brokerage_fee AS desiredBrokerageFee, " +
            "ST_DistanceSphere(p.coordinate, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) AS distanceInMeters " +
            "FROM properties p " +
            "WHERE p.status = :#{#status.name()} " +
            "AND NOT EXISTS (SELECT 1 FROM bids b WHERE b.property_id = p.id AND b.agent_user_id = :agentId) " +
            "ORDER BY distanceInMeters ASC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<PropertyDistanceProjection> findBiddableByAgentOrderByDistance(
            @Param("status") PropertyStatus status,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("agentId") Long agentId,
            @Param("limit") int limit
    );

    /**
     * 찜 개수 증가
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Property p SET p.favoriteCount = p.favoriteCount + 1 WHERE p.id = :propertyId")
    void increaseFavoriteCount(@Param("propertyId") Long propertyId);

    /**
     * 찜 개수 감소 (0 미만으로 떨어지지 않게 보호)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Property p SET p.favoriteCount = p.favoriteCount - 1 WHERE p.id = :propertyId AND p.favoriteCount > 0")
    void decreaseFavoriteCount(@Param("propertyId") Long propertyId);

    /**
     * 신고 횟수 증가,
     * 신고 5회 이상 시 의심 매물 자동 전환
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Property p " +
            "SET p.reportCount = p.reportCount + 1, " +
            "    p.isSuspicious = (CASE WHEN p.reportCount + 1 >= 5 THEN true ELSE p.isSuspicious END) " +
            "WHERE p.id = :propertyId")
    void increaseReportCountAndCheckSuspicious(@Param("propertyId") Long propertyId);

    /**
     * 허위매물 자동탐지 규칙 - 같은 주소+상세주소(즉 같은 호실)를 다른 소유자가 동시에 등록한 경우를 찾는다.
     * 같은 유저가 본인 매물을 수정/재등록하는 경우는 제외한다.
     */
    @Query("SELECT p FROM Property p WHERE p.address = :address AND p.detailAddress = :detailAddress " +
            "AND p.id <> :excludePropertyId AND p.user.id <> :excludeUserId AND p.status <> :excludedStatus")
    List<Property> findConflictingAddressListings(
            @Param("address") String address,
            @Param("detailAddress") String detailAddress,
            @Param("excludePropertyId") Long excludePropertyId,
            @Param("excludeUserId") Long excludeUserId,
            @Param("excludedStatus") PropertyStatus excludedStatus
    );

    /**
     * 중개사 매칭(acceptBid)에서 동시 수락 막는 잠금
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Property p WHERE p.id = :id")
    Optional<Property> findByIdWithPessimisticLock(@Param("id") Long id);


    /**
     * 관리자용 신고된 매물 목록. 신고 많은 순 정렬(의심 매물은 reportCount>=5라 자연히 위쪽에 몰림).
     * 이미 삭제 처리된 매물은 더 조치할 게 없으므로 제외. 소유자 ID를 같이 내려주므로 user를 함께 fetch
     */
    @EntityGraph(attributePaths = "user")
    List<Property> findByReportCountGreaterThanAndStatusNotOrderByReportCountDesc(Integer reportCount, PropertyStatus excludedStatus);

    /**
     * 급등 랭킹 조회용. Redis에는 삭제된 매물의 ID가 여전히 남아있을 수 있으므로 여기서 걸러낸다
     */
    List<Property> findByIdInAndStatusNot(List<Long> ids, PropertyStatus excludedStatus);

    /**
     * 중개사 현장 인증용: 매물 좌표와 중개사 현재 GPS 좌표 사이의 거리(미터) 계산
     */
    @Query(value = "SELECT ST_DistanceSphere(coordinate, :realtorLocation) FROM properties WHERE id = :propertyId", nativeQuery = true)
    Double calculateDistanceToProperty(@Param("propertyId") Long propertyId, @Param("realtorLocation") Point realtorLocation);
}
