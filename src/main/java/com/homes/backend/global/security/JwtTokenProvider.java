package com.homes.backend.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key signingKey;
    private final long validityInMilliseconds = 3600000; // 1시간

    // 토큰 재발급 기능에 사용할 수명 설정 (Access: 30분, Refresh: 14일)
    private static final long ACCESS_TOKEN_EXPIRATION = 30 * 60 * 1000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 14 * 24 * 60 * 60 * 1000L;

    // 생성자를 통해 yml에 있는 jwt.secret 값 가져오기
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 기존 단독 토큰 굽는 메서드 (호환성 유지)
    public String createToken(Long userId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(email) // 토큰 주인의 이메일
                .claim("userId", userId) // 토큰 내부에 고유 ID 숨겨놓기
                .setIssuedAt(now)
                .setExpiration(validity) // 만료 시간 설정
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 1. 토큰에서 유저 이메일(Subject) 끄집어냄
    public String getEmailFromToken(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("email", String.class);
    }

    // 2. 토큰에서 유저 고유 ID(Long)를 안전하게 파싱해서 꺼냄
    // createTokenSet 에서 Subject 에 String 으로 박았으므로 문자열을 다시 Long 으로 파싱
    public Long getUserId(String token) {
        String subject = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return Long.parseLong(subject);
    }

    // 3. 토큰이 기한이 만료되지 않았는지, 유효한지 검사
    public boolean validateToken(String token) {
        try {
            io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 4. 토큰의 만료 시간까지 남은 밀리초(ms)를 계산하는 메서드
    public Long getExpiration(String token) {
        java.util.Date expiration = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        long now = new java.util.Date().getTime();
        return (expiration.getTime() - now);
    }

    // Access + Refresh 토큰 세트 발급
    public TokenDto createTokenSet(Long userId, String email, String role) {
        long now = (new java.util.Date()).getTime();
        java.util.Date accessTokenExpiresIn = new java.util.Date(now + ACCESS_TOKEN_EXPIRATION);
        java.util.Date refreshTokenExpiresIn = new java.util.Date(now + REFRESH_TOKEN_EXPIRATION);

        // 1. Access Token 생성 (role 클레임 포함: 프론트가 로그인 직후 중개사/일반 유저 UI를 분기할 수 있도록)
        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .setExpiration(accessTokenExpiresIn)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        // 2. Refresh Token 생성
        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setExpiration(refreshTokenExpiresIn)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenExpirationTime(REFRESH_TOKEN_EXPIRATION)
                .build();
    }
}