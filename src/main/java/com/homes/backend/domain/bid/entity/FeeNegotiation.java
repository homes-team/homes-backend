package com.homes.backend.domain.bid.entity;

import com.homes.backend.domain.realtor.entity.Agent;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fee_negotiations")
public class FeeNegotiation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "negotiation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 집주인이 역제안을 보낸 경우 채워짐
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id") // 중개사가 제안을 보낸 경우 채워짐
    private Agent agent;

    @Column(nullable = false)
    private Double suggestedFee; // 제안 수수료(%)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    /**
     * 집주인이 제안할 때 쓰는 생성자
     */
    @Builder(builderMethodName = "builderByUser")
    public FeeNegotiation(Bid bid, User user, Double suggestedFee) {
        this.bid = bid;
        this.user = user;
        this.suggestedFee = suggestedFee;
        this.status = BidStatus.PENDING;
    }

    /**
     * 중개사가 제안할 때 쓰는 생성자
     */
    @Builder(builderMethodName = "builderByAgent")
    public FeeNegotiation(Bid bid, Agent agent, Double suggestedFee) {
        this.bid = bid;
        this.agent = agent;
        this.suggestedFee = suggestedFee;
        this.status = BidStatus.PENDING;
    }
}
