package com.project.stock.investory.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentSimpleResponseDto {
    private Long commentId;  // 댓글 ID
    private Long postId;         // 게시글 ID
    private String content;  // 댓글 내용
    private LocalDateTime createdAt;
}
