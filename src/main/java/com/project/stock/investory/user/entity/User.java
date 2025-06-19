package com.project.stock.investory.user.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(length = 255)
    private String password;  // 비밀번호 (소셜 로그인 시 null 가능)

    @Column(nullable = false, length = 50)
    private String name; // 이름

    @Column(nullable = true, length = 20)
    private String phone; // 전화번호

    @Column(nullable = false, columnDefinition = "NUMBER(1)")
    @Builder.Default
    private Integer isSocial = 0;  // 소셜 로그인 여부 (0: 일반, 1: 소셜)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 가입일

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일

    private LocalDateTime deletedAt;  // 탈퇴일 (soft delete)

    // 최초 저장 시 실행 (회원가입 시점)
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 수정 시 실행 (회원정보 수정 등)
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 사용자 정보 수정 메서드
    public void updateInfo(String name, String phone) {
        this.name = name;
        this.phone = phone != null ? phone : this.phone;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void withdraw() {
        this.deletedAt = LocalDateTime.now();
    }

}
