package com.homes.backend.domain.bid.entity;

public enum BidStatus {
    PENDING,    // 대기 (초기 상태)
    ACCEPTED,   // 수락 (매칭 완료)
    REJECTED,   // 거절됨 (집주인이 PENDING 상태에서 거절)
    WITHDRAWN,   // 철회됨 (중개사가 PENDING 상태에서 스스로 거둬들임 - 패널티 없음)
    CANCELLED   // 수락됐다가 매칭 자체가 취소됨 (패널티의 대상이 될 수 있음)
}
