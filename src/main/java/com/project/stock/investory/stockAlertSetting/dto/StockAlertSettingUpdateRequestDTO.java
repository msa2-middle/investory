package com.project.stock.investory.stockAlertSetting.dto;

import com.project.stock.investory.stockAlertSetting.model.ConditionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAlertSettingUpdateRequestDTO {

    @NotBlank
    @Min(value = 1, message = "targetPrice는 1 이상이어야 합니다.")
    private int targetPrice;

    @NotNull(message = "조건을 입력해주세요.")
    private ConditionType condition;

}