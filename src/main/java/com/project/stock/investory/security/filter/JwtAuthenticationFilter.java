package com.project.stock.investory.security.filter;

import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.security.jwt.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. HTTP Header에서 JWT 추출
        String header = request.getHeader("Authorization");

        // Authorization 헤더가 존재하고, "Bearer "로 시작하는 경우에만 처리
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 이후 토큰 문자열만 추출

            try {
                // 2. 토큰 유효성 검사
                if (jwtUtil.validateToken(token)) {
                    // 3. 토큰에서 사용자 정보 추출
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String email = jwtUtil.getEmailFromToken(token);
                    String name = jwtUtil.getNameFromToken(token);

                    // 4. CustomUserDetails 생성
                    CustomUserDetails userDetails = new CustomUserDetails(userId, email, name);

                    // 5. 인증 객체 생성 및 SecurityContext에 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException e) {
                //log.warn("JWT 검증 실패: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
                return;
            }
        }
//        else {
//            // JWT 헤더 없으면 anonymous 토큰 넣어줌
//            SecurityContextHolder.getContext().setAuthentication(
//                    new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList())
//            );
//        }

        // 6. 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }
}
