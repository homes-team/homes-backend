package com.homes.backend.domain.user.dto.request;

import com.homes.backend.domain.user.entity.UserReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "유저 신고 요청 DTO")
public record UserReportCreateReqDto(
        @Schema(description = "신고 사유 선택 (HARASSMENT, SCAM_ATTEMPT, SPAM, INAPPROPRIATE_CONTENT, OTHER)", example = "OTHER")
        @NotNull(message = "신고 사유는 필수입니다.")
        UserReportReason reason,

        @Schema(description = "기타(OTHER) 사유 선택 시 직접 입력하는 내용", example = "채팅으로 계속 다른 사이트 링크를 보냅니다.")
        @Size(max = 500, message = "customReason은 500자를 초과할 수 없습니다.")
        String customReason,

        @Schema(description = "어느 채팅방에서 있었던 일인지(선택)", example = "12")
        Long chatRoomId
) {
}
