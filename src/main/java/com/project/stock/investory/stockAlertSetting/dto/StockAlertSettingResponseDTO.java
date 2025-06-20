package com.project.stock.investory.stockAlertSetting.dto;


import com.project.stock.investory.stockAlertSetting.model.ConditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockAlertSettingResponseDTO {

    private Long userId;
    private String stockId;
    private int targetPrice;
    private ConditionType condition;

}
