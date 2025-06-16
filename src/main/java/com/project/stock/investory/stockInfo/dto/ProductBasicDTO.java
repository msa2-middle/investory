package com.project.stock.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품기본정보
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductBasicDTO {
    private String pdno;                   // 종목코드
    private String prdtName;              // 종목명
    private String prdtEngName;           // 종목 영문명
    private String prdtAbrvName;          // 약어명
    private String prdtSaleStatCd;        // 판매 상태
    private String prdtRiskGradCd;        // 위험 등급
    private String prdtClsfName;          // 상품 분류명 (ETF, 주식 등)
    private String ivstPrdtTypeCdName;    // 투자 상품 유형
    private String frstErlmDt;            // 최초 등록일자
}