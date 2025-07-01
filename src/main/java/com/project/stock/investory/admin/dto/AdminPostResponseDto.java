package com.project.stock.investory.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminPostResponseDto {
    private Long postId;
    private String title;
    private String authorName;
    private LocalDateTime createdAt;
}
