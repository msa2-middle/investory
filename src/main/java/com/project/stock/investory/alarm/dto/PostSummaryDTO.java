package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSummaryDTO {
    private Long postId;
    private String title;
    private String authorName;
    private Integer likeCount;
    private Integer viewCount;

    public static PostSummaryDTO from(Post post) {
        return PostSummaryDTO.builder()
                .postId(post.getPostId())  // 실제 필드명 사용
                .title(post.getTitle())
                .authorName("작성자")  // Post에 직접 User 연관관계가 없으므로 임시값
                .likeCount(post.getLikeCount())
                .viewCount(post.getViewCount())
                .build();
    }
}