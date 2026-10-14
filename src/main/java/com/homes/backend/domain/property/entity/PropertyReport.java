package com.homes.backend.domain.property.entity;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) //created_at 자동 생성
@Table(
        name="report",
        uniqueConstraints={
                @UniqueConstraint(
                        name="uk_property_reporter",
                        columnNames = {"property_id","reporter_id"} // 중복 신고 방지
                )
        }
)
public class PropertyReport extends BaseEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="report_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition = "TEXT")
    private ReportReason reason;

    @Column(length = 500)
    private String customReason; // 기타 사유 입력 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Builder
    public PropertyReport(ReportReason reason,String customReason, Property property, User reporter) {
        this.reason = reason;
        this.customReason = customReason;
        this.property = property;
        this.reporter = reporter;
    }
}
