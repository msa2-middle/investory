// StockPriceEvent.java
package com.project.stock.investory.stockAlertSetting.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockPriceEvent {
    private final String stockCode;
    private final int currentPrice;
}