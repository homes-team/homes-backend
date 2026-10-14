package com.homes.backend.domain.property.repository;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.property.entity.PropertyReport;
import com.homes.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyReportRepository extends JpaRepository<PropertyReport,Long> {
    /**
     * 이미 신고한 매물인지 확인
     */
    boolean existsByPropertyAndReporter(Property property, User reporter);

    /**
     * 내가 신고한 내역 리스트 조회
     */
    List<PropertyReport> findAllByReporterId(Long reporterId);

    /**
     * 회원 탈퇴 시 본인이 신고한 내역 일괄 삭제
     */
    void deleteAllByReporterId(Long reporterId);

    /**
     * 관리자용 특정 매물의 신고 상세 내역
     */
    @EntityGraph(attributePaths = "reporter")
    List<PropertyReport> findAllByPropertyIdOrderByCreatedAtDesc(Long propertyId);
}
