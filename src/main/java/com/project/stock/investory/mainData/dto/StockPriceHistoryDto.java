package com.project.stock.investory.mainData.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.stock.investory.mainData.entity.StockPriceHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceHistoryDto {
    // 거래일자 (예: "20250131")
    @JsonProperty("stck_bsop_date")
    private String stckBsopDate;

    // 종가
    @JsonProperty("stck_clpr")
    private String stckClpr;

    // 시가
    @JsonProperty("stck_oprc")
    private String stckOprc;

    // 고가
    @JsonProperty("stck_hgpr")
    private String stckHgpr;

    // 저가
    @JsonProperty("stck_lwpr")
    private String stckLwpr;

    // 누적 거래량
    @JsonProperty("acml_vol")
    private String acmlVol;

    // 누적 거래대금
    @JsonProperty("acml_tr_pbmn")
    private String acmlTrPbmn;

    // 플로팅 클래스 코드
    @JsonProperty("flng_cls_code")
    private String flngClsCode;

    // 배당률
    @JsonProperty("prtt_rate")
    private String prttRate;

    // 수정여부
    @JsonProperty("mod_yn")
    private String modYn;

    // 전일 대비 부호 (상승/하락 등)
    @JsonProperty("prdy_vrss_sign")
    private String prdyVrssSign;

    // 전일 대비 가격
    @JsonProperty("prdy_vrss")
    private String prdyVrss;

    // 재발행 사유 (공란 가능)
    @JsonProperty("revl_issu_reas")
    private String revlIssuReas;


    /**
     * DTO를 Entity로 변환하는 메서드
     *
     * @param stockId 저장할 종목코드 (외래키)
     * @return StockPriceHistory Entity
     */
    public StockPriceHistory toEntity(String stockId) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        return StockPriceHistory.builder()
                .stockId(stockId)
                .tradeDate(LocalDate.parse(this.stckBsopDate, formatter))
                .openPrice(parseInteger(this.stckOprc))
                .closePrice(parseInteger(this.stckClpr))
                .highPrice(parseInteger(this.stckHgpr))
                .lowPrice(parseInteger(this.stckLwpr))
                .volume(parseLong(this.acmlVol))
                .build();
    }

    // String을 Integer로 안전하게 파싱하는 헬퍼 메서드
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Integer 파싱 오류: " + value);
            return null;
        }
    }

    // String을 Long으로 안전하게 파싱하는 헬퍼 메서드
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Long 파싱 오류: " + value);
            return null;
        }
    }


}
