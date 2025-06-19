package com.project.stock.investory.config;

import com.project.stock.investory.user.service.CustomOAuth2UserService;
import com.project.stock.investory.security.filter.JwtAuthenticationFilter;
import com.project.stock.investory.security.handler.OAuth2LoginSuccessHandler;
import com.project.stock.investory.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil; // JWT 토큰 생성, 검증
    private final CustomOAuth2UserService customOAuth2UserService; // 소셜 로그인 사용자 DB 처리
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler; // 소셜 로그인 성공 후 JWT 발급

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Swagger, 회원가입, 로그인 등 공개 API
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/users/signup", "/users/login", "/oauth2/**"
                        ).permitAll()

                        // stock 관련 GET 요청은 전체 공개
                        .requestMatchers(HttpMethod.GET, "/stock/**").permitAll()

                        // community posts 단일 조회 공개
                        .requestMatchers(HttpMethod.GET, "/community/posts/**").permitAll()

                        // 메인화면 Data 전체 공개
                        .requestMatchers(HttpMethod.GET, "/main/**").permitAll()

                        // 알림 조회 공개
                        .requestMatchers(HttpMethod.GET, "/alarm/storage/**").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)  // 소셜 로그인 성공 후 추가 작업 (→ JWT 발급)
                )
                // JWT 필터 등록
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // 비밀번호 암호화를 위한 Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
