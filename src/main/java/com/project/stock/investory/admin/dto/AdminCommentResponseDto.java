package com.project.stock.investory.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCommentResponseDto {
    private Long commentId;
    private Long postId;
    private String postTitle;
    private Long userId;
    private String userName;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;
}
