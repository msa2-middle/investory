package com.project.stock.investory.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oauth_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OAuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oauth_user_seq_generator")
    @SequenceGenerator(name = "oauth_user_seq_generator", sequenceName = "oauth_user_seq", allocationSize = 1)
    private Long oauthUserId;

    @Column(nullable = false, length = 50)
    private String provider; // 예: "kakao", "naver"

    @Column(nullable = false, length = 100)
    private String providerUserId; // 소셜 사용자 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 연결된 일반 유저
}
