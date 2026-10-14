package com.homes.backend.domain.notification.repository;

import com.homes.backend.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 개별 읽음 처리. 내 알림이 맞는지까지 조건에 포함해서, 남의 알림 ID를 넣어도 0건 처리되게 한다.
     * 반환값(영향받은 row 수)으로 서비스단에서 "존재하지 않거나 내 알림이 아님"을 판별한다.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :notificationId AND n.user.id = :userId")
    int markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsRead(@Param("userId") Long userId);
}
