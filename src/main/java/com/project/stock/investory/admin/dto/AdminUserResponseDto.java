package com.project.stock.investory.admin.dto;

import com.project.stock.investory.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponseDto {
    private Long userId;
    private String email;
    private String name;
    private String phone;
    private String role;
    private LocalDateTime createdAt;

    public static AdminUserResponseDto fromEntity(User user) {
        return AdminUserResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

