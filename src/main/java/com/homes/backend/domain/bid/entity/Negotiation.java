package com.homes.backend.domain.bid.entity;

import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "negotiations")
public class Negotiation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid; // 어떤 제안서에서 이루어진 협상인지

    @Column(nullable = false)
    private String senderRole; // USER(집주인이 제안) 또는 AGENT(중개사가 제안)

    @Column(nullable = false)
    private Double suggestedFee; // 이번에 새로 제시한 수수료

    @Column(columnDefinition = "TEXT")
    private String message; // 전달 메시지 (예: "0.1%만 깎아주시면 바로 계약할게요")

    @Builder
    public Negotiation(Bid bid, String senderRole, Double suggestedFee, String message) {
        this.bid = bid;
        this.senderRole = senderRole;
        this.suggestedFee = suggestedFee;
        this.message = message;
    }
}
