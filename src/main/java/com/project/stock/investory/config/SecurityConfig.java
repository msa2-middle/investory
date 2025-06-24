package com.project.stock.investory.config;

import com.project.stock.investory.security.handler.CustomAuthenticationEntryPoint;
import com.project.stock.investory.user.service.CustomOAuth2UserService;
import com.project.stock.investory.security.filter.JwtAuthenticationFilter;
import com.project.stock.investory.security.handler.OAuth2LoginSuccessHandler;
import com.project.stock.investory.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil; // JWT 토큰 생성, 검증
    private final CustomOAuth2UserService customOAuth2UserService; // 소셜 로그인 사용자 DB 처리
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler; // 소셜 로그인 성공 후 JWT 발급
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF, 기본 폼로그인, HTTP Basic 비활성화 (우리는 JWT 사용)
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 인증 실패 시 커스텀 핸들러
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))

                // 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // Swagger, 회원가입, 로그인 등 공개 API
                        .requestMatchers(
                                "/swagger-ui/**", "/v3/api-docs/**",
                                "/users/signup", "/users/login", "/oauth2/**", "/alarm/**",
                                "/users/password-reset/send-code",
                                "/users/password-reset/verify-code",
                                "/users/password-reset/reset"
                        ).permitAll()

                        // stock 관련 GET 요청은 전체 공개
                        .requestMatchers(HttpMethod.GET, "/stock/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/stock/*/analytics/**").permitAll()

                        // community posts 조회 공개
                        .requestMatchers(HttpMethod.GET, "/community/posts/**").permitAll()

                        // 메인화면 전체 공개
                        .requestMatchers(HttpMethod.GET, "/main/**").permitAll()

                        // 알림 조회 공개
                        .requestMatchers(HttpMethod.GET, "/alarm/storage/**").permitAll()

                        // 댓글 조회 GET 요청 전체 공개
                        .requestMatchers(HttpMethod.GET, "/post/**").permitAll()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                .anonymous(Customizer.withDefaults())

                // 소셜 로그인 설정 (로그인 성공 시 JWT 발급)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                // JWT 필터 등록
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // 비밀번호 암호화 설정
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}