package com.project.stock.investory.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentSimpleResponseDto {
    private Long commentId;  // 댓글 ID
    private String content;  // 댓글 내용
}
