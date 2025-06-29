package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.alarm.entity.AlarmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlarmRequestDTO {
    private String content;
    private AlarmType type;

    // 주가 알람 설정
    private String stockId;

    // 게시글 댓글, 좋아요
    private Long postId;

    // 댓글 좋아요
    private Long commentId;
}
