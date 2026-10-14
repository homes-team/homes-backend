package com.homes.backend.domain.property.dto.response;

import com.homes.backend.domain.property.entity.PropertyReport;

import java.time.LocalDateTime;

public record ReportListRespDto(
        Long reportId,
        Long propertyId,
        String propertyTitle,
        String reasonDescription,
        LocalDateTime reportedAt
) {
    public static ReportListRespDto from(PropertyReport report){
        return new ReportListRespDto(
                report.getId(),
                report.getProperty().getId(),
                report.getProperty().getTitle(),
                report.getReason().getDescription(),
                report.getCreatedAt()
        );
    }
}
