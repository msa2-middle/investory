package com.project.stock.investory.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRefreshResponseDto {
    private String accessToken;
    private String refreshToken;
}
