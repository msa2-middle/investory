package com.project.stock.investory.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId; // 회원번호

    @Column(nullable = false, unique = true, length = 255)
    private String email; // 이메일

    @Column(nullable = true, length = 255)
    private String password;  // 비밀번호(소셜 로그인 시 null 가능)

    @Column(nullable = false, length = 50)
    private String name; // 이름

    @Column(nullable = false, length = 20)
    private String phone; // 전화번호

    @Column(nullable = false, columnDefinition = "NUMBER(1) DEFAULT 0") // DB에 들어갈 실제 정의 명시 (기본값 포함)
    @Builder.Default // @Builder를 쓸 때도 이 필드의 기본값을 유지하도록 보장
    private Boolean isSocial = false;  // 소셜 로그인 여부

    @Column(nullable = false)
    private LocalDateTime createdAt; // 가입일

    @Column(nullable = false)
    private LocalDateTime updatedAt; // 수정일

    private LocalDateTime deletedAt;  // 탈퇴일(soft delete)
}
