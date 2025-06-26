package com.project.stock.investory.stockInfo.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RealTimeTradeDTO {
    private String stockId;          // 종목코드 (005930 등)
    private String tradePrice;       // 체결가
    private String tradeVolume;      // 체결량
    private String changeRate;       // 등락률
    private String accumulateVolume; // 누적거래량
    private String tradeTime;        // HHmmss
}
