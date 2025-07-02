package com.project.stock.investory.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentResponseDTO {

    private Long commentId;
    private Long userId;
    private String userName;
    private Long postId;
    private String content;
    private Integer likeCount;
    private LocalDateTime createdAt;

}
