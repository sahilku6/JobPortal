package com.jobportal.auth.config;

import com.jobportal.auth.dto.AuthDto;
import com.jobportal.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component @RequiredArgsConstructor @Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();

        try {
            String providerId = resolveProviderId(provider, oAuth2User);
            String email = oAuth2User.getAttribute("email");
            String name  = oAuth2User.getAttribute("name");

            AuthDto.AuthResponse authResponse = authService.processOAuthLogin(provider, providerId, email, name);

            String redirectUrl = frontendBaseUrl + "/oauth/callback"
                + "?accessToken="  + URLEncoder.encode(authResponse.getAccessToken(),  StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("OAuth login failed provider={}: {}", provider, e.getMessage(), e);
            response.sendRedirect(frontendBaseUrl + "/login?error=oauth_failed");
        }
    }

    private String resolveProviderId(String provider, OAuth2User user) {
        Object id = "github".equals(provider) ? user.getAttribute("id") : user.getAttribute("sub");
        if (id == null) throw new IllegalStateException("Cannot resolve OAuth provider ID for " + provider);
        return id.toString();
    }
}
