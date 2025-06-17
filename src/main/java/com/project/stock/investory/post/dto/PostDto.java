package com.project.stock.investory.post.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long postId;
    private Long userId;
    private Long boardId;
    private String title;         // 제목 추가
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}