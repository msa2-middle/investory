package com.project.stock.investory.user.entity;

import com.project.stock.investory.user.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_generator")
    @SequenceGenerator(name = "user_seq_generator", sequenceName = "user_seq", allocationSize = 1)
    private Long userId; // 회원번호

    @Column(nullable = false, unique = true, length = 255)
    private String email; // 이메일

    @Column(length = 255, nullable = true)
    private String password;  // 비밀번호 (소셜 로그인 시 null 가능)

    @Column(nullable = false, length = 50)
    private String name; // 이름

    @Column(nullable = true, length = 20)
    private String phone; // 전화번호

    @Column(nullable = false, columnDefinition = "NUMBER(1)")
    @Builder.Default
    private Integer isSocial = 0;  // 소셜 로그인 여부 (0: 일반, 1: 소셜)

    @Column(name = "refresh_token", length = 512, nullable = true)
    private String refreshToken; // 리프레시 토큰

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 가입일 (자동 생성)

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일 (자동 수정)

    private LocalDateTime deletedAt;  // 탈퇴일 (soft delete)

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    // 사용자 정보 수정 메서드
    public void updateInfo(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    // 비밀번호 변경 메서드
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 익명화 메서드 추가
    public void anonymize() {
        this.name = "탈퇴한 회원";
        this.email = "withdrawn_" + this.userId + "@example.com";
        this.phone = null;
        this.password = null;
        this.refreshToken = null;
    }

    // 탈퇴 처리 (soft delete)
    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
        this.anonymize();
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    public void enableSocialLogin() {
        this.isSocial = 1;
    }

    public void setRole(Role role) {
        this.role = role;
    }

}
