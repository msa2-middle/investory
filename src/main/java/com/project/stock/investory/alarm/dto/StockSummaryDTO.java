package com.project.stock.investory.alarm.dto;

import com.project.stock.investory.stockInfo.model.Stock;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockSummaryDTO {
    private String stockId;  // String 타입으로 변경
    private String stockName;

    public static StockSummaryDTO from(Stock stock) {
        return StockSummaryDTO.builder()
                .stockId(stock.getStockId())  // 실제 필드명 사용
                .stockName(stock.getStockName())  // 실제 필드명 사용
                .build();
    }
}