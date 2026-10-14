package com.homes.backend.domain.chat.entity;

import com.homes.backend.domain.property.entity.Property;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms", uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_room_participants", columnNames = {"property_id", "user_id", "agent_user_id"}))
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 집주인(회원) 측

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_user_id", nullable = false)
    private User agentUser; // 중개사 계정의 User (Agent.agent_id가 아니라 User.id를 참조)

    @Column(name = "is_user_left", nullable = false)
    private boolean userLeft = false;

    @Column(name = "is_agent_left", nullable = false)
    private boolean agentLeft = false;

    @ColumnDefault("false") // 이미 데이터가 있는 테이블에 컬럼 추가 시 NOT NULL 위반 방지
    @Column(name = "is_suspended", nullable = false)
    private boolean suspended = false; // 관리자가 정지시킨 채팅방 - 새 메시지 전송만 막고 기존 대화 내역은 보존

    @Builder
    public ChatRoom(Property property, User user, User agentUser) {
        this.property = property;
        this.user = user;
        this.agentUser = agentUser;
    }

    /**
     * 나갔던 쪽이 있어도 방을 다시 활성 상태로 되돌림
     */
    public void reopen() {
        this.userLeft = false;
        this.agentLeft = false;
    }

    /**
     * callerId가 회원 쪽인지 중개사 쪽인지 판별 (실제 나가기 처리는 컬럼 단위 원자적 UPDATE로 수행 - ChatRoomRepository 참고)
     */
    public boolean isUserSide(Long callerId) {
        return this.user.getId().equals(callerId);
    }

    public boolean isMember(Long userId) {
        return this.user.getId().equals(userId) || this.agentUser.getId().equals(userId);
    }

    /**
     * 관리자용 채팅방 정지 - 새 메시지 전송만 막는다. 기존 메시지 내역은 그대로 조회 가능
     */
    public void suspend() {
        this.suspended = true;
    }
}
