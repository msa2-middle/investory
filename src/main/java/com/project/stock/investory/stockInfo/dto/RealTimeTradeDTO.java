package com.project.stock.investory.stockInfo.dto;

import com.fasterxml.jackson.databind.JsonNode;
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


    /* --------------------------------------------------------------------- */
    /*  정적 팩토리                                                           */
    /* --------------------------------------------------------------------- */

    /**
     * H0STCNT0 raw “fields[]” 배열을 DTO 로 변환.
     * <pre>
     * 0  종목코드
     * 1  체결시간(HHmmss)
     * 2  현재가
     * 5  등락률
     * 12 체결량
     * 13 누적거래량
     * </pre>
     */
    public static RealTimeTradeDTO from(String[] f) {
        return new RealTimeTradeDTO(
                f[0],          // stockId
                f[2],          // tradePrice
                f[12],         // tradeVolume
                f[5] + "%",    // changeRate (문자열 붙이기)
                f[13],         // accumulateVolume
                f[1]           // tradeTime
        );
    }

    // ✅ 정적 팩토리 메서드 추가
    public static RealTimeTradeDTO from(String stockId, JsonNode body) {
        RealTimeTradeDTO dto = new RealTimeTradeDTO();
        dto.setStockId(stockId);
        dto.setTradePrice(body.path("stck_prpr").asText());
        dto.setTradeVolume(body.path("cntg_vol").asText());
        dto.setChangeRate(body.path("prdy_ctrt").asText());
        dto.setAccumulateVolume(body.path("acml_vol").asText());
        dto.setTradeTime(body.path("trdq_tms").asText());
        return dto;
    }
}
