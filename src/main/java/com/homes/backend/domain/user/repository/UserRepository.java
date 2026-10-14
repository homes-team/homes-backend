package com.homes.backend.domain.user.repository;

import com.homes.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    /**
     * 신고 횟수 증가, 신고 5회 이상 시 의심 유저 자동 전환 (PropertyRepository와 동일 패턴)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u " +
            "SET u.reportCount = u.reportCount + 1, " +
            "    u.isSuspicious = (CASE WHEN u.reportCount + 1 >= 5 THEN true ELSE u.isSuspicious END) " +
            "WHERE u.id = :userId")
    void increaseReportCountAndCheckSuspicious(@Param("userId") Long userId);

    /**
     * 관리자용 신고된 유저 목록. 신고 많은 순 정렬. 이미 탈퇴 처리된 유저는 더 조치할 게 없으므로 제외
     */
    List<User> findByReportCountGreaterThanAndDeletedAtIsNullOrderByReportCountDesc(Integer reportCount);
}