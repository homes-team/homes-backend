package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyStatus;
import com.homes.backend.domain.property.entity.RecentView;
import com.homes.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

    /**
     * 기존 시청 기록 찾기 (Upsert 검사용)
     */
    Optional<RecentView> findByUserAndProperty(User user, Property property);

    /**
     * 내가 최근 본 방 목록 최신순 조회(Top 20개 조회). 이후 삭제(DELETED)된 매물은 제외
     */
    List<RecentView> findTop20ByUserIdAndPropertyStatusNotOrderByViewedAtDesc(Long userId, PropertyStatus status);

    /**
     * 최근 본 방 기록 저장 및 시간 갱신 (네이티브 UPSERT)
     * * 사용자가 동일 매물을 동시에 여러 번 조회할 때
     * 발생할 수 있는 동시성 이슈 및 유니크 제약조건 위반을 방지합니다.
     */
    @Modifying
    @Query(value = "INSERT INTO recent_view (user_id, property_id, viewed_at, created_at, updated_at) " +
            "VALUES (:userId, :propertyId, NOW(), NOW(), NOW()) " +
            "ON CONFLICT (user_id, property_id) " +
            "DO UPDATE SET viewed_at = NOW(), updated_at = NOW()",
            nativeQuery = true)
    void upsertRecentView(@Param("userId") Long userId, @Param("propertyId") Long propertyId);

    /**
     * 회원 탈퇴 시 본인의 최근 본 방 기록 일괄 삭제
     */
    void deleteAllByUserId(Long userId);
}
