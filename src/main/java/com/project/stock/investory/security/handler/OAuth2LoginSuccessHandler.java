package com.project.stock.investory.security.handler;

import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import com.project.stock.investory.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.URLEncoder;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = (String) oauthUser.getAttributes().get("email");

        // JWT 발급할 때 userId 필요
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("로그인 사용자를 찾을 수 없습니다."));

        // JWT 토큰 발급 (userId, email, name)
        String accessToken = jwtUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getName()
        );

        // 소셜 로그인 성공 후 프론트엔드로 토큰 전달
        response.sendRedirect("http://localhost:5173/oauth-success?token=" + accessToken+ "&name=" + URLEncoder.encode(user.getName(), "UTF-8"));
    }
}
