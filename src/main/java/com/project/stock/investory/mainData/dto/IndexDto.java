package com.project.stock.investory.mainData.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IndexDto {
    // output1 필드 전체 매핑
    @JsonProperty("bstp_nmix_prdy_vrss")
    private String prdyVrss; // 전일 대비

    @JsonProperty("prdy_vrss_sign")
    private String vrssSign; // 대비 부호

    @JsonProperty("bstp_nmix_prdy_ctrt")
    private String prdyCtrt; // 전일 대비율

    @JsonProperty("prdy_nmix")
    private String prdyNmix; // 전일 지수

    @JsonProperty("acml_vol")
    private String acmlVol; // 누적 거래량

    @JsonProperty("acml_tr_pbmn")
    private String acmlTrPbmn; // 누적 거래대금

    @JsonProperty("hts_kor_isnm")
    private String korIsnm; // 종목명

    @JsonProperty("bstp_nmix_prpr")
    private String prpr; // 현재가

    @JsonProperty("bstp_cls_code")
    private String clsCode; // 업종 코드

    @JsonProperty("prdy_vol")
    private String prdyVol; // 전일 거래량

    @JsonProperty("bstp_nmix_oprc")
    private String oprc; // 시가

    @JsonProperty("bstp_nmix_hgpr")
    private String hgpr; // 고가

    @JsonProperty("bstp_nmix_lwpr")
    private String lwpr; // 저가

    @JsonProperty("futs_prdy_oprc")
    private String futsOprc; // 선물 시가

    @JsonProperty("futs_prdy_hgpr")
    private String futsHgpr; // 선물 고가

    @JsonProperty("futs_prdy_lwpr")
    private String futsLwpr; // 선물 저가
}