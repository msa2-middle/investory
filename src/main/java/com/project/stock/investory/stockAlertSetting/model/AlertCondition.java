package com.project.stock.investory.stockAlertSetting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AlertCondition {
    private final Long settingId;
    private final Long userId;
    private final String stockCode;
    private final int targetPrice;
    private final ConditionType condition;
}