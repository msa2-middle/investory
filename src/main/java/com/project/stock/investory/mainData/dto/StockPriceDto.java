package com.project.stock.investory.mainData.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StockPriceDto {
    private Output output;
    private String rt_cd;
    private String msg_cd;
    private String msg1;

    @Data
    public static class Output {
        @JsonProperty("stck_prpr")
        private String price; // 주식 현재가

        @JsonProperty("prdy_vrss")
        private String dayOverDayChange; // 전일 대비

        @JsonProperty("prdy_vrss_sign")
        private String changeSign; // 1:상승, 2:하락

        @JsonProperty("stck_oprc")
        private String openPrice; // 시가

        @JsonProperty("stck_hgpr")
        private String highPrice; // 고가

        @JsonProperty("stck_lwpr")
        private String lowPrice; // 저가

        @JsonProperty("acml_vol")
        private String volume; // 누적 거래량

        // 필요시 추가 필드 구현
    }
}
