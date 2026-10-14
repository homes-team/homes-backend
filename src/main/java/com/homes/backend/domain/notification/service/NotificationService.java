package com.homes.backend.domain.notification.service;

import com.homes.backend.domain.notification.dto.response.NotificationResDto;
import com.homes.backend.domain.notification.entity.Notification;
import com.homes.backend.domain.notification.entity.NotificationType;
import com.homes.backend.domain.notification.event.NotificationCreatedEvent;
import com.homes.backend.domain.notification.exception.NotificationErrorCode;
import com.homes.backend.domain.notification.repository.NotificationRepository;
import com.homes.backend.domain.notification.sse.SseEmitterRepository;
import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.exception.UserErrorCode;
import com.homes.backend.domain.user.repository.UserRepository;
import com.homes.backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L; // 30분

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterRepository sseEmitterRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<NotificationResDto> getMyNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResDto::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * 채팅 메시지 도착, 매칭 확정 등 다른 도메인 서비스에서 호출해서 알림을 생성한다.
     * push는 여기서 바로 하지 않고 이벤트로만 발행한다 - 이 메서드는 보통 호출자(ChatService.sendMessage() 등)의
     * 트랜잭션에 합류해서 실행되는데, 그 트랜잭션이 커밋되기 전에 SSE로 먼저 push해버리면
     * (1) 커밋 전이라 다른 요청에서 아직 조회가 안 되는 알림을 클라이언트가 먼저 받거나
     * (2) 이후 같은 트랜잭션이 롤백되면 "저장도 안 된 알림"을 클라이언트가 받는 문제가 생길 수 있다.
     */
    @Transactional
    public void createNotification(Long userId, NotificationType type, String content, Long referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .content(content)
                .user(user)
                .type(type)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationCreatedEvent(userId, NotificationResDto.from(notification)));
    }

    // 트랜잭션이 실제로 커밋된 이후에만 push한다. DB 접근이 없는 메서드라 클래스 레벨 @Transactional을 명시적으로 배제해야 함
    // (@TransactionalEventListener는 자체 트랜잭션에 편입되는 걸 허용하지 않음 - NOT_SUPPORTED 필수)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        push(event.userId(), event.notification());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        sseEmitterRepository.save(userId, emitter);

        emitter.onCompletion(() -> sseEmitterRepository.remove(userId, emitter));
        emitter.onTimeout(() -> sseEmitterRepository.remove(userId, emitter));
        emitter.onError(e -> sseEmitterRepository.remove(userId, emitter));

        try {
            // 연결 직후 더미 이벤트를 보내지 않으면 일부 프록시/브라우저가 응답 없는 연결로 보고 바로 끊어버림
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            completeWithErrorAndRemove(userId, emitter, e);
        }

        return emitter;
    }

    private void push(Long userId, NotificationResDto notification) {
        for (SseEmitter emitter : sseEmitterRepository.findByUserId(userId)) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException e) {
                completeWithErrorAndRemove(userId, emitter, e);
            }
        }
    }

    /**
     * send() 실패 시 내 저장소에서만 빼면 끝이 아니다 - emitter.completeWithError()로 비동기 요청 자체를
     * "에러로 종료됐다"고 프레임워크에 명시적으로 알려줘야 한다. 안 그러면 원래 이 SSE 요청을 처리하던
     * HTTP 스레드 쪽에서 나중에 별도로(뒤늦게) 같은 예외를 미처리 에러로 다시 로깅하게 된다.
     */
    private void completeWithErrorAndRemove(Long userId, SseEmitter emitter, IOException e) {
        log.warn("SSE 전송 실패, 연결 종료 및 제거: userId={}", userId);
        emitter.completeWithError(e);
        sseEmitterRepository.remove(userId, emitter);
    }
}
