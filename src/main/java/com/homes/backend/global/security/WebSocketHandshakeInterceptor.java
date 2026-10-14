package com.homes.backend.global.security;

import com.homes.backend.domain.user.entity.User;
import com.homes.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 웹소켓은 REST처럼 매 요청마다 인증 필터가 도는 게 아니라, 연결을 맺는 이 시점(핸드셰이크)에 딱 한 번만 인증 기회가 있다.
 * 여기서 검증한 유저 정보를 attributes에 담아두면, 스프링이 세션 전체에 걸쳐 계속 들고 다녀준다.
 *
 * 접속 URL에는 JWT 원본이 아니라 WebSocketTicketService가 발급한 1회용 단기 티켓만 노출한다
 * (URL 쿼리 파라미터는 프록시/리버스프록시 로그, 브라우저 히스토리 등에 평문으로 남을 수 있어서, 진짜 액세스 토큰을 거기 실으면 위험함).
 */
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final WebSocketTicketService webSocketTicketService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ticket = extractTicket(request);

        if (!StringUtils.hasText(ticket)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long userId = webSocketTicketService.consumeTicket(ticket);
        if (userId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put("userId", user.getId());
        attributes.put("role", user.getRole());

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // 별도 후처리 없음
    }

    // 브라우저 기본 WebSocket API는 커스텀 헤더를 못 붙이므로, 접속 URL의 쿼리 파라미터로 티켓을 받는다 (예: /ws/chats?ticket=xxx)
    private String extractTicket(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            if (param.startsWith("ticket=")) {
                return param.substring("ticket=".length());
            }
        }
        return null;
    }
}
