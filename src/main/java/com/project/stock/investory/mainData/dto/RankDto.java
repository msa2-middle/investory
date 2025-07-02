package com.project.stock.investory.mainData.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RankDto {

    @JsonProperty("code")
    private String code;           // 종목 코드

    @JsonProperty("name")
    private String name;           // 종목명

    @JsonProperty("daebi")
    private String daebi;          // 전일 대비 상태 (1:상승, 2:하락, 5:보합)

    @JsonProperty("price")
    private String price;          // 현재가

    @JsonProperty("chgrate")
    private String chgrate;        // 전일 대비 등락률 (%)

    @JsonProperty("acml_vol")
    private String acmlVol;        // 누적 거래량

    @JsonProperty("trade_amt")
    private String tradeAmt;       // 거래대금

    @JsonProperty("change")
    private String change;         // 전일 대비 가격 변화

    @JsonProperty("cttr")
    private String cttr;           // 체결 강도

    @JsonProperty("open")
    private String open;           // 시가

    @JsonProperty("high")
    private String high;           // 고가

    @JsonProperty("low")
    private String low;            // 저가

    @JsonProperty("high52")
    private String high52;         // 52주 최고가

    @JsonProperty("low52")
    private String low52;          // 52주 최저가

    @JsonProperty("expprice")
    private String expPrice;       // 예상 체결가

    @JsonProperty("expchange")
    private String expChange;      // 예상 체결가 대비 변화

    @JsonProperty("expchggrate")
    private String expChgGrate;    // 예상 체결가 대비 등락률

    @JsonProperty("expcvol")
    private String expCVol;        // 예상 체결량

    @JsonProperty("chgrate2")
    private String chgRate2;       // 기준 대비 등락률

    @JsonProperty("expdaebi")
    private String expDaebi;       // 예상 전일 대비 상태

    @JsonProperty("recprice")
    private String recPrice;       // 추천가

    @JsonProperty("uplmtprice")
    private String uplmtPrice;     // 상한가

    @JsonProperty("dnlmtprice")
    private String dnlmtPrice;     // 하한가

    @JsonProperty("stotprice")
    private String stotPrice;      // 총 거래대금
}
