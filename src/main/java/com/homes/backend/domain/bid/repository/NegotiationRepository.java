package com.homes.backend.domain.bid.repository;

import com.homes.backend.domain.bid.entity.Negotiation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {
    List<Negotiation> findAllByBidIdOrderByCreatedAtAsc(Long bidId); //과거 -> 최신 순으로 핑퐁 기록 보여주기

    /**
     * 여러 제안서(bid)의 최신 역제안 금액을 한 번에 조회하기 위한 배치 조회
     * (최신순으로 내려오므로, bidId별 첫 등장 값이 최신 역제안가)
     */
    List<Negotiation> findAllByBidIdInOrderByCreatedAtDesc(List<Long> bidIds);
}
