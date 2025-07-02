package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.entity.RelatedEntityType;
import com.project.stock.investory.user.entity.User;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmRequestDTO {
    private AlarmType type;
    private String content;
    private String targetUrl;
    private String relatedEntityId;
    private RelatedEntityType relatedEntityType;
    private User sender;

    // 정적 팩토리 메서드들
    public static AlarmRequestDTO forPost(AlarmType type, String content, String targetUrl,
                                          Long postId, User sender) {
        return AlarmRequestDTO.builder()
                .type(type)
                .content(content)
                .targetUrl(targetUrl)
                .relatedEntityId(String.valueOf(postId))  // Long → String 변환
                .relatedEntityType(RelatedEntityType.POST)
                .sender(sender)
                .build();
    }

    public static AlarmRequestDTO forComment(AlarmType type, String content, String targetUrl,
                                             Long commentId, User sender) {
        return AlarmRequestDTO.builder()
                .type(type)
                .content(content)
                .targetUrl(targetUrl)
                .relatedEntityId(String.valueOf(commentId))  // Long → String 변환
                .relatedEntityType(RelatedEntityType.COMMENT)
                .sender(sender)
                .build();
    }

    public static AlarmRequestDTO forStock(AlarmType type, String content, String targetUrl,
                                           String stockId) {
        return AlarmRequestDTO.builder()
                .type(type)
                .content(content)
                .targetUrl(targetUrl)
                .relatedEntityId(stockId)  // String 그대로 사용
                .relatedEntityType(RelatedEntityType.STOCK)
                .build();
    }
}
