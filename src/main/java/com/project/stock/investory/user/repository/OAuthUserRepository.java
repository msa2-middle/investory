package com.project.stock.investory.user.repository;

import com.project.stock.investory.user.entity.OAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthUserRepository extends JpaRepository<OAuthUser, Long> {
    // 소셜 로그인 계정 정보로 OAuthUser 조회
    Optional<OAuthUser> findByProviderAndProviderUserId(String provider, String providerUserId);
}
