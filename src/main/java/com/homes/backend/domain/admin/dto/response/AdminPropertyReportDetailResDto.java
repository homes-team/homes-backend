package com.homes.backend.domain.admin.dto.response;

import com.homes.backend.domain.property.entity.PropertyReport;

import java.time.LocalDateTime;

public record AdminPropertyReportDetailResDto(
        Long reportId,
        String reasonDescription,
        String customReason,
        Long reporterId,
        String reporterName,
        String reporterEmail,
        LocalDateTime reportedAt
) {
    public static AdminPropertyReportDetailResDto from(PropertyReport report) {
        return new AdminPropertyReportDetailResDto(
                report.getId(),
                report.getReason().getDescription(),
                report.getCustomReason(),
                report.getReporter().getId(),
                report.getReporter().getName(),
                report.getReporter().getEmail(),
                report.getCreatedAt()
        );
    }
}
