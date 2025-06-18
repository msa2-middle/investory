package com.project.stock.investory.post.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

/**
 * 클라이언트(프론트엔드) → 서버로 "게시글 작성" 요청 시
 * 전달되는 데이터를 담는 DTO(Data Transfer Object)
 * */
public class RequestDto {
    private String title;
    private String content;
}