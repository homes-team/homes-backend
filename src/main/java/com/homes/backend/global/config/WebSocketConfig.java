package com.homes.backend.global.config;

import com.homes.backend.domain.chat.websocket.ChatChannelInterceptor;
import com.homes.backend.global.security.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 실시간 채팅 설정.
 * 명세서상 URL은 /ws/chats/{chatId}(방마다 별도 소켓)이지만, STOMP는 소켓 하나를 여러 방이 공유하고
 * 목적지(destination)로 방을 구분하는 방식이 표준이라 /ws/chats 하나만 열고 /topic/chats/{chatId}로 방을 구분한다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    private final ChatChannelInterceptor chatChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chats")
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOriginPatterns("*"); // 나중에 배포 시 프론트 도메인만 허용하도록 수정 (CorsConfig.java와 동일 방침)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 서버 -> 클라이언트 브로드캐스트 (구독)
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트 -> 서버 전송
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatChannelInterceptor);
    }
}
