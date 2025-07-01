package com.project.stock.investory.stockAlertSetting.dto;


import com.project.stock.investory.stockAlertSetting.model.ConditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockAlertSettingResponseDTO {

    private Long userId;
    private String stockId;
    private int targetPrice;
    private ConditionType condition;
    private LocalDateTime createdAt;

}
