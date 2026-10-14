package com.homes.backend.domain.notification.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 유저별로 열려있는 SSE 연결(여러 탭/기기 동시 접속 가능)을 메모리에 들고 있는 저장소.
 * 서버 인스턴스가 여러 대로 늘어나면(수평 확장) 이 방식으론 부족해지고 Redis Pub/Sub 등이 필요하지만,
 * 지금 규모에서는 과한 설계라 인메모리로 충분하다.
 */
@Component
public class SseEmitterRepository {

    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public void save(Long userId, SseEmitter emitter) {
        emittersByUserId.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /**
     * emitter를 빼고, 리스트가 비면 맵 엔트리 자체를 지운다 (그대로 두면 접속/해제가 반복될 때마다 빈 리스트가 계속 쌓임).
     * get() 후 별도로 지우는 방식은 그 사이 다른 스레드의 save()가 만든 새 리스트를 통째로 날릴 수 있어서,
     * compute()로 조회-수정-삭제를 한 번에 원자적으로 처리한다.
     */
    public void remove(Long userId, SseEmitter emitter) {
        emittersByUserId.compute(userId, (key, emitters) -> {
            if (emitters == null) {
                return null;
            }
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    public List<SseEmitter> findByUserId(Long userId) {
        return emittersByUserId.getOrDefault(userId, List.of());
    }
}
