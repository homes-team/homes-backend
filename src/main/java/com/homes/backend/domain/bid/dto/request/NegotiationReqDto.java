package com.homes.backend.domain.bid.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NegotiationReqDto (
        @NotNull(message = "제안 수수료를 입력해 주세요.")
        @Positive(message = "수수료는 0보다 커야 합니다.")
        Double suggestedFee, // 새로 제안하는 수수료
        String message
){

}
