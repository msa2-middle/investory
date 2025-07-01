package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.alarm.entity.Alarm;
import com.project.stock.investory.alarm.entity.AlarmType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmResponseDTO {
    private Long alarmId;
    private AlarmType type;
    private String content;
    private Integer isRead;
    private String targetUrl;

    // 관련 엔티티 정보
    private String relatedEntityId;
    private String relatedEntityType;

    // 발신자 정보
    private Long senderId;
    private String senderName;

    // 연관 엔티티의 요약 정보 (동적으로 로드)
    private Object relatedEntityInfo;

    private LocalDateTime createdAt;

    public static AlarmResponseDTO from(Alarm alarm) {
        return AlarmResponseDTO.builder()
                .alarmId(alarm.getAlarmId())
                .type(alarm.getType())
                .content(alarm.getContent())
                .isRead(alarm.getIsRead())
                .targetUrl(alarm.getTargetUrl())
                .relatedEntityId(alarm.getRelatedEntityId())
                .relatedEntityType(alarm.getRelatedEntityType().name())
                .senderId(alarm.getSender() != null ? alarm.getSender().getUserId() : null)
                .senderName(alarm.getSender() != null ? alarm.getSender().getName() : null)
                .createdAt(alarm.getCreatedAt())
                .build();
    }

    public static AlarmResponseDTO fromWithRelatedInfo(Alarm alarm, Object relatedEntityInfo) {
        AlarmResponseDTO dto = from(alarm);
        dto.relatedEntityInfo = relatedEntityInfo;
        return dto;
    }
}
