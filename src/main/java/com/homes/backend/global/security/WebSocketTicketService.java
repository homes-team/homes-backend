package com.homes.backend.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 웹소켓 핸드셰이크용 1회용 단기 티켓 발급/검증.
 * 접속 URL 쿼리 파라미터에 JWT 원본을 그대로 노출하면 프록시/리버스프록시 로그, 브라우저 히스토리 등에
 * 30분짜리 진짜 액세스 토큰이 평문으로 남을 수 있어서, 대신 30초짜리 1회용 티켓만 노출시킨다.
 */
@Service
@RequiredArgsConstructor
public class WebSocketTicketService {

    private static final String TICKET_PREFIX = "WS_TICKET:";
    private static final long TICKET_TTL_SECONDS = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    public String issueTicket(Long userId) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TICKET_PREFIX + ticket, String.valueOf(userId), TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        return ticket;
    }

    /**
     * 조회와 동시에 삭제해서 1회용으로 만든다. 이미 쓰였거나 만료됐으면 null.
     */
    public Long consumeTicket(String ticket) {
        String key = TICKET_PREFIX + ticket;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        redisTemplate.delete(key);
        return Long.parseLong((String) value);
    }
}
