package com.project.stock.investory.user.service;

import com.project.stock.investory.user.entity.OAuthUser;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.OAuthUserRepository;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuthUserRepository oAuthUserRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerUserId;
        String email;
        String name;

        if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            providerUserId = String.valueOf(attributes.get("id"));
            email = (String) kakaoAccount.get("email");
            name = (String) profile.get("nickname");
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            providerUserId = (String) response.get("id");
            email = (String) response.get("email");
            name = (String) response.get("name");
        } else if ("google".equals(registrationId)) {
            providerUserId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
        }

        if (email == null || providerUserId == null) {
            throw new IllegalArgumentException("소셜 로그인에서 필수 정보 누락");
        }

        // 이메일 기준으로 User 먼저 찾기
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .password(null)
                            .name(name)
                            .isSocial(1)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        // OAuthUser 테이블 중복 확인 후 없으면 저장
        oAuthUserRepository.findByProviderAndProviderUserId(registrationId, providerUserId)
                .orElseGet(() -> {
                    OAuthUser oauthUser = OAuthUser.builder()
                            .provider(registrationId)
                            .providerUserId(providerUserId)
                            .user(user)
                            .build();
                    return oAuthUserRepository.save(oauthUser);
                });

        Map<String, Object> extendedAttributes = new HashMap<>(attributes);
        extendedAttributes.put("email", email);

        // 소셜 로그인 사용자 인증 객체 생성
        // DefaultOAuth2User 생성 시 권한 정보(authorities)가 필수이므로 최소 권한 ROLE_USER 부여
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                extendedAttributes,
                "email"
        );
    }
}
