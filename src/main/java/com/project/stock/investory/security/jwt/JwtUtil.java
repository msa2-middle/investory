package com.project.stock.investory.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private final Key key; // JWT 서명(Signature)용 비밀키
    private static final long EXPIRATION_MS = 1000 * 60 * 60; // 1시간
    // RefreshToken 만료 기간
    private static final long REFRESH_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 14;

    // application.properties에서 jwt.secret 값을 주입받음
    public JwtUtil(@Value("${jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // 토큰 생성 (userId, email, name 포함)
    public String generateToken(Long userId, String email, String name, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 사용자 ID
                .claim("email", email)              // 이메일 추가 정보
                .claim("name", name)        // 이름 추가 정보
                .claim("role", role)
                .setIssuedAt(new Date())            // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명
                .compact();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰에서 userId 추출
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 토큰에서 email 추출
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    // 토큰에서 name 추출
    public String getNameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("name", String.class);
    }

    // 내부 공통 Claims 파싱 메서드
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateRefreshToken(Long userId, String email, String name, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("name", name)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

}
