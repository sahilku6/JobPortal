package com.jobportal.auth.config;

import com.jobportal.auth.security.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .oauth2Login(oauth -> oauth.successHandler(oAuth2SuccessHandler)
                .failureUrl("/api/v1/auth/oauth/failure"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
                    "/api/v1/auth/request-email-otp", "/api/v1/auth/verify-email-otp",
                    "/api/v1/auth/forgot-password/**",
                    "/login/oauth2/**", "/oauth2/**", "/api/v1/auth/oauth/failure"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/role/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
