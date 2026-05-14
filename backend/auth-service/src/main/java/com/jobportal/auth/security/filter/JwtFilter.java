package com.jobportal.auth.security.filter;

import com.jobportal.auth.repository.UserRepository;
import com.jobportal.auth.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Single-session enforcement: on every login a new JTI is saved to
 * users.current_token_jti. Any token with a non-matching JTI is rejected,
 * so logging in on a new device/browser automatically invalidates all other sessions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil        jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            log.warn("Invalid or expired JWT: {}", request.getRequestURI());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        try {
            Claims claims   = jwtUtil.extractClaims(token);
            String role     = claims.get("role",  String.class);
            String email    = claims.get("email", String.class);
            Long   userId   = Long.valueOf(claims.getSubject());
            String tokenJti = claims.getId();

            // Single-session check: only access tokens carry a JTI.
            if (tokenJti != null) {
                boolean jtiValid = userRepository.findById(userId)
                    .map(u -> tokenJti.equals(u.getCurrentTokenJti()))
                    .orElse(false);

                if (!jtiValid) {
                    log.warn("JTI mismatch userId={} — old session token rejected", userId);
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Session invalidated. Please log in again.");
                    return;
                }
            }

            String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email, null, List.of(new SimpleGrantedAuthority(authority)));
            authentication.setDetails(userId);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("JWT processing error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token processing error");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
