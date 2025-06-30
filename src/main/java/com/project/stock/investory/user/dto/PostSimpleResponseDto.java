package com.project.stock.investory.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostSimpleResponseDto {
    private Long postId;
    private String title;
    private LocalDateTime createdAt;
}
