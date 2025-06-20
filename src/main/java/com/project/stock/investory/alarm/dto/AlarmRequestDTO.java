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
}
