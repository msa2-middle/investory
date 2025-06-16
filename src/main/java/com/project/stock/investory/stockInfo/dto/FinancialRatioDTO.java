package com.project.stock.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 재무 비율
 */
@Getter
@Setter
@NoArgsConstructor
public class FinancialRatioDTO {
    private String stacYymm;        // 결산 년월
    private String grs;             // 매출액 증가율
    private String bsopPrfiInrt;    // 영업 이익 증가율
    private String ntinInrt;        // 순이익 증가율
    private String roeVal;          // ROE
    private String eps;             // EPS
    private String sps;             // 주당매출액
    private String bps;             // BPS
    private String rsrvRate;        // 유보 비율
    private String lbltRate;        // 부채 비율
}
