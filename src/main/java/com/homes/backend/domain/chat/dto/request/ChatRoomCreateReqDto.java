package com.homes.backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "채팅방 생성 요청 DTO")
public record ChatRoomCreateReqDto(
        @Schema(description = "채팅 상대와 관련된 매물 ID", example = "1")
        @NotNull(message = "propertyId는 필수입니다.")
        Long propertyId,

        @Schema(description = "채팅할 중개사의 realtorId(=agentId)", example = "1")
        @NotNull(message = "realtorId는 필수입니다.")
        Long realtorId
) {}
