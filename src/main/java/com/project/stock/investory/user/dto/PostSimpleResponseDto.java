package com.project.stock.investory.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSimpleResponseDto {
    private Long postId;
    private String title;
}
