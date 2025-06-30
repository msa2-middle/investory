package com.project.stock.investory.post.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostWithAuthorDto {
    private Long postId;
    private String title;
    private String content;
    private String name;

}
