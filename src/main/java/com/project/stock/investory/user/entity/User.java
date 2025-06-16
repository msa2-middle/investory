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

    @Column(nullable = false, length = 20)
    private String phone; // 전화번호

    @Column(nullable = false)
    @Builder.Default
    private Integer isSocial = 0;  // 소셜 로그인 여부 (0: 일반, 1: 소셜)

    @Column(nullable = false)
    private LocalDateTime createdAt; // 가입일

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일

    private LocalDateTime deletedAt;  // 탈퇴일 (soft delete)
}
