package com.project.stock.investory.mainData.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
