package com.project.stock.investory.alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertSettingResponseDTO extends AlarmResponseDTO {
    private String stockId;
}
