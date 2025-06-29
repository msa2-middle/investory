package com.project.stock.investory.commentLike.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class CommentLikeResponseDTO {
    private boolean isLiked;
    private int likeCount;
}
