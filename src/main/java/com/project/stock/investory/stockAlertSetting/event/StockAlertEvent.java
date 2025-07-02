// StockAlertEvent.java
package com.project.stock.investory.stockAlertSetting.event;

import com.project.stock.investory.stockAlertSetting.model.ConditionType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockAlertEvent {
    private final String action; // "ADD", "REMOVE", "UPDATE", "REFRESH"
    private final String stockCode;
    private final Long settingId;
    private final ConditionType conditionType;
    private final Integer targetPrice;

    // ADD용 생성자
    public static StockAlertEvent createAdd(String stockCode) {
        return new StockAlertEvent("ADD", stockCode, null, null, null);
    }

    // REMOVE용 생성자
    public static StockAlertEvent createRemove(String stockCode, Long settingId, ConditionType conditionType, Integer targetPrice) {
        return new StockAlertEvent("REMOVE", stockCode, settingId, conditionType, targetPrice);
    }

    // UPDATE용 생성자 (기존 조건 제거 후 새 조건 추가)
    public static StockAlertEvent createUpdate(String stockCode) {
        return new StockAlertEvent("UPDATE", stockCode, null, null, null);
    }

    // REFRESH용 생성자
    public static StockAlertEvent createRefresh() {
        return new StockAlertEvent("REFRESH", null, null, null, null);
    }
}