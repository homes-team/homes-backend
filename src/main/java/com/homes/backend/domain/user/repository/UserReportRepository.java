package com.homes.backend.domain.user.repository;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.entity.UserReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    /**
     * 이미 신고한 유저인지 확인
     */
    boolean existsByReportedUserAndReporter(User reportedUser, User reporter);

    /**
     * 관리자용 특정 유저의 신고 상세 내역
     */
    @EntityGraph(attributePaths = "reporter")
    List<UserReport> findAllByReportedUserIdOrderByCreatedAtDesc(Long reportedUserId);
}
