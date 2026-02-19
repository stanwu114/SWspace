package com.aispace.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * JWT 工具类
 * 用于生成和验证 JWT Token
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${aispace.security.jwt.secret:aispace-default-jwt-secret-key-must-be-at-least-256-bits-long}")
    private String jwtSecret;

    @Value("${aispace.security.jwt.expiration:86400000}") // 默认 24 小时
    private long jwtExpiration;

    @Value("${aispace.security.jwt.enabled:true}")
    private boolean jwtEnabled;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token
     */
    public String generateToken(String username) {
        return generateToken(username, new HashMap<>());
    }

    /**
     * 生成 JWT Token（带额外声明）
     */
    public String generateToken(String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }

        return builder.compact();
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从 Token 获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        if (!jwtEnabled) {
            return true; // JWT 禁用时不验证
        }

        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("无效的 JWT Token");
        } catch (ExpiredJwtException ex) {
            log.error("JWT Token 已过期");
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的 JWT Token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims 字符串为空");
        } catch (Exception ex) {
            log.error("JWT Token 验证失败: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 检查 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新 Token
     */
    public String refreshToken(String token) {
        String username = getUsernameFromToken(token);
        if (username != null) {
            return generateToken(username);
        }
        return null;
    }

    /**
     * JWT 是否启用
     */
    public boolean isJwtEnabled() {
        return jwtEnabled;
    }
}
