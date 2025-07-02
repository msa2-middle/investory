package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.comment.model.Comment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentSummaryDTO {
    private Long commentId;
    private String content;
    private String authorName;
    private Long postId;
    private String postTitle;

    public static CommentSummaryDTO from(Comment comment) {
        return CommentSummaryDTO.builder()
                .commentId(comment.getCommentId())  // 실제 필드명 사용
                .content(comment.getContent().length() > 50 ?
                        comment.getContent().substring(0, 50) + "..." : comment.getContent())
                .authorName(comment.getUser().getName())  // User 연관관계 사용
                .postId(comment.getPost().getPostId())  // 실제 필드명 사용
                .postTitle(comment.getPost().getTitle())
                .build();
    }
}