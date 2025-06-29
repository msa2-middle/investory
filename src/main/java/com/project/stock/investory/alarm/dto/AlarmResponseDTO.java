package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.alarm.entity.AlarmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class AlarmResponseDTO {
    private Long alarmId;
    private String content;
    private AlarmType type;
    private Integer isRead;
}
