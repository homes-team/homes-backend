package com.homes.backend.domain.admin.dto.response;

import com.homes.backend.domain.user.entity.UserReport;

import java.time.LocalDateTime;

public record AdminUserReportDetailResDto(
        Long userReportId,
        String reasonDescription,
        String customReason,
        Long reporterId,
        String reporterName,
        String reporterEmail,
        Long chatRoomId,
        LocalDateTime reportedAt
) {
    public static AdminUserReportDetailResDto from(UserReport report) {
        return new AdminUserReportDetailResDto(
                report.getId(),
                report.getReason().getDescription(),
                report.getCustomReason(),
                report.getReporter().getId(),
                report.getReporter().getName(),
                report.getReporter().getEmail(),
                report.getChatRoom() != null ? report.getChatRoom().getId() : null,
                report.getCreatedAt()
        );
    }
}
