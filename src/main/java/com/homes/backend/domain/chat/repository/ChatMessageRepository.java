package com.homes.backend.domain.chat.repository;

import com.homes.backend.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // createdAt만으로는 동일 시각에 온 메시지의 순서가 불안정할 수 있어 id를 2차 정렬 키로 추가
    List<ChatMessage> findAllByRoomIdOrderByCreatedAtAscIdAsc(Long roomId);

    /**
     * 상대방(callerId가 아닌 쪽)이 보낸, 아직 안 읽은 메시지를 조회 시점에 읽음 처리
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true " +
            "WHERE m.room.id = :roomId AND m.sender.id != :callerId AND m.read = false")
    void markMessagesAsRead(@Param("roomId") Long roomId, @Param("callerId") Long callerId);
}
