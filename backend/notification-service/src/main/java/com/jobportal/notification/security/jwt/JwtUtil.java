package com.jobportal.notification.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Lightweight JWT utility for the notification-service.
 * Used only to extract userId from the ?token= query param on the SSE endpoint
 * (EventSource cannot send Authorization headers).
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extracts the userId (subject) from a JWT token.
     * Returns null if the token is invalid or expired.
     */
    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        return extractUserId(token) != null;
    }
}
