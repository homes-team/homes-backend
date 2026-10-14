package com.homes.backend.domain.property.entity;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "recent_view",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_property_view",
                        columnNames = {"user_id", "property_id"} // 중복 저장 방지
                )
        }
)
public class RecentView extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recent_view_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @Builder
    public RecentView(User user, Property property) {
        this.user = user;
        this.property = property;
        this.viewedAt = LocalDateTime.now();
    }

    /**
     * 이미 본 방을 또 봤을 때 시간 갱신
     */
    public void updateViewTime() {
        this.viewedAt = LocalDateTime.now();
    }
}
