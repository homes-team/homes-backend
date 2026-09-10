package com.homes.backend.domain.bid.repository;

import com.homes.backend.domain.bid.entity.Bid;
import com.homes.backend.domain.bid.entity.BidStatus;
import com.homes.backend.domain.property.entity.PropertyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // 특정 매물에 달린 입찰 목록 중 "집주인이 지금 고를 수 있는" 진행 중인(PENDING/ACCEPTED) 것만 최신순으로 조회
    // 취소/거절된 이력은 이 목록에서 제외한다 (같은 중개사가 재입찰하면 옛 취소 건과 새 대기 건이 중복으로 보이는 걸 방지)
    @EntityGraph(attributePaths = {"agent"})
    List<Bid> findAllByPropertyIdAndStatusInOrderByCreatedAtDesc(Long propertyId, List<BidStatus> statuses);

    /**
     * 이 매물을 "담당"하는 중개사 조회 - 수락된(ACCEPTED) 입찰은 매물당 최대 1건이라는 전제
     */
    @EntityGraph(attributePaths = {"agent"})
    Optional<Bid> findByPropertyIdAndStatus(Long propertyId, BidStatus status);

    /**
     * 담당 중개사의 다른 매물 조회용 - 현재 매물은 제외하고, 삭제된 매물도 제외
     */
    @Query("SELECT b FROM Bid b JOIN FETCH b.property p " +
            "WHERE b.agent.id = :agentId AND b.status = :status " +
            "AND p.id <> :excludePropertyId AND p.status <> :excludedPropertyStatus " +
            "ORDER BY b.createdAt DESC")
    List<Bid> findOtherAcceptedBidsByAgent(
            @Param("agentId") Long agentId,
            @Param("status") BidStatus status,
            @Param("excludePropertyId") Long excludePropertyId,
            @Param("excludedPropertyStatus") PropertyStatus excludedPropertyStatus
    );

    /**
     * 해당 매물에 해당 중개사의 "진행 중인"(취소/거절되지 않은) 제안서가 존재하는지 검사
     */
    boolean existsByPropertyIdAndAgentIdAndStatusIn(Long propertyId, Long agentId, List<BidStatus> statuses);

    /**
     * 중개사 마이페이지 통계 - 이번 달 거래(수락 확정 + 거래완료) 건수
     */
    @Query("SELECT COUNT(b) FROM Bid b " +
            "WHERE b.agent.id = :agentId AND b.status = :bidStatus " +
            "AND b.property.status = :propertyStatus " +
            "AND b.property.dealCompletedAt >= :start AND b.property.dealCompletedAt < :exclusiveEnd")
    long countCompletedDealsInRange(
            @Param("agentId") Long agentId,
            @Param("bidStatus") BidStatus bidStatus,
            @Param("propertyStatus") PropertyStatus propertyStatus,
            @Param("start") LocalDateTime start,
            @Param("exclusiveEnd") LocalDateTime exclusiveEnd
    );

    /**
     * 중개사 마이페이지 통계 - 매물 종류별 평균 확정 수수료 (전체 기간)
     */
    @Query("SELECT b.property.propertyType AS propertyType, AVG(b.finalFee) AS averageFee " +
            "FROM Bid b " +
            "WHERE b.agent.id = :agentId AND b.finalFee IS NOT NULL " +
            "GROUP BY b.property.propertyType")
    List<AgentFeeByPropertyTypeProjection> findAverageFeeByPropertyType(@Param("agentId") Long agentId);

    /**
     * 중개사 성사율 계산 - 전체 입찰 건수 / 상태별 입찰 건수
     */
    long countByAgentId(Long agentId);

    long countByAgentIdAndStatus(Long agentId, BidStatus status);

    /**
     * 매칭 완료 되지 못한 입찰 제안서 거절로 상태 변환
     */
    @Modifying
    @Query("UPDATE Bid b SET b.status = 'REJECTED' WHERE b.property.id = :propertyId AND b.id != :acceptedBidId AND b.status = 'PENDING'")
    void rejectOtherPendingBids(@Param("propertyId") Long propertyId, @Param("acceptedBidId") Long acceptedBidId);
}
