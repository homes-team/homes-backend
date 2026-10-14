package com.homes.backend.domain.bid.entity;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bids")
public class Bid extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    @Column(nullable = false)
    private Double proposedFee; // 중개사가 매물 입찰할 때 역으로 제안하는 수수료(%)

    @Column(columnDefinition = "TEXT")
    private String content; // 중개사 어필 메시지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_user_id", nullable = false)
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property; // 대상 매물

    @Column(name = "final_fee")
    private Double finalFee; // 확정 수수료(%)

    public void acceptBid(Double finalFee) {
        this.status = BidStatus.ACCEPTED;
        this.finalFee = finalFee; // 최종 수수료를 저장
    }

    public void cancelBid() {
        this.status = BidStatus.CANCELLED;
    }

    // 중개사가 스스로 PENDING 제안서를 철회(취소)
    public void withdraw() {
        this.status = BidStatus.WITHDRAWN;
    }

    // 집주인이 PENDING 제안서를 거절
    public void reject() {
        this.status = BidStatus.REJECTED;
    }

    @Builder
    public Bid(Double proposedFee, String content, Agent agent, Property property) {
        this.proposedFee = proposedFee;
        this.content = content;
        this.agent = agent;
        this.property = property;
        this.status = BidStatus.PENDING;
    }
}
