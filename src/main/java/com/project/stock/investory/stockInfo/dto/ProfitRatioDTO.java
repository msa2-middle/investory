package com.project.stock.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수익성비율
 */
@Setter
@Getter
@NoArgsConstructor
public class ProfitRatioDTO {
    private String stacYymm;            // 결산 년월
    private String cptlNtinRate;        // 총자본 순이익률
    private String selfCptlNtinInrt;    // 자기자본 순이익률
    private String saleNtinRate;        // 매출액 순이익률
    private String saleTotlRate;        // 매출액 총이익률
}