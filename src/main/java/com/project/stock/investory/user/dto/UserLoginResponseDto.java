package com.project.stock.investory.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserLoginResponseDto {
    private Long userId;
    private String email;
    private String name;
    private String accessToken;  // JWT 추가
}
