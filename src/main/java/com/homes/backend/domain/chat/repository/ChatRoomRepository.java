package com.homes.backend.domain.chat.repository;

import com.homes.backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * (매물, 회원, 중개사) 조합으로 이미 만들어진 채팅방이 있는지 조회 - 중복 생성 방지용
     */
    Optional<ChatRoom> findByPropertyIdAndUserIdAndAgentUserId(Long propertyId, Long userId, Long agentUserId);

    /**
     * 내 채팅방 목록 - 내가 회원 쪽이든 중개사 쪽이든 조회되지만, 내가 이미 나간 방은 내 목록에서 제외한다.
     * (상대방이 나갔는지 여부와는 무관하게, 내가 안 나갔으면 계속 보임)
     */
    @Query("SELECT r FROM ChatRoom r WHERE " +
            "(r.user.id = :userId AND r.userLeft = false) OR " +
            "(r.agentUser.id = :userId AND r.agentLeft = false)")
    List<ChatRoom> findActiveRoomsByParticipantId(@Param("userId") Long userId);

    /**
     * 나가기 처리 - 컬럼 단위 원자적 UPDATE. 두 참여자가 동시에 나가도 서로 다른 컬럼을 건드리므로 lost update가 없다.
     */
    @Modifying
    @Query("UPDATE ChatRoom r SET r.userLeft = true WHERE r.id = :roomId")
    void markUserLeft(@Param("roomId") Long roomId);

    @Modifying
    @Query("UPDATE ChatRoom r SET r.agentLeft = true WHERE r.id = :roomId")
    void markAgentLeft(@Param("roomId") Long roomId);

    /**
     * 두 유저 사이에 실제로 메시지를 주고받은 채팅방이 있는지 확인 (유저 신고 시, 접촉한 적 있는 사람만 신고 가능하도록 검증).
     * 채팅방은 메시지 없이도 생성될 수 있어서(자동 삭제 로직 없음), 단순히 방 존재 여부만으로는 부족하고
     * 실제 메시지 교환 이력까지 확인해야 한다.
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ChatRoom r WHERE " +
            "((r.user.id = :userId1 AND r.agentUser.id = :userId2) OR " +
            "(r.user.id = :userId2 AND r.agentUser.id = :userId1)) " +
            "AND EXISTS (SELECT 1 FROM ChatMessage m WHERE m.room = r)")
    boolean existsRoomBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
