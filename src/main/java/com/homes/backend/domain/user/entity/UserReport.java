package com.homes.backend.domain.user.entity;

import com.homes.backend.domain.chat.entity.ChatRoom;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reported_user_reporter",
                        columnNames = {"reported_user_id", "reporter_id"} // 중복 신고 방지
                )
        }
)
public class UserReport extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_report_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "TEXT")
    private UserReportReason reason;

    @Column(length = 500)
    private String customReason; // 기타 사유 입력 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom; // 어느 채팅방에서 있었던 일인지 (선택)

    @Builder
    public UserReport(UserReportReason reason, String customReason, User reportedUser, User reporter, ChatRoom chatRoom) {
        this.reason = reason;
        this.customReason = customReason;
        this.reportedUser = reportedUser;
        this.reporter = reporter;
        this.chatRoom = chatRoom;
    }
}
