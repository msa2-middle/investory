package com.project.stock.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 대차대조표
 */
@Getter
@Setter
@NoArgsConstructor
public class BalanceSheetDTO {
    private String stacYymm;       // 결산 년월
    private String cras;           // 유동자산
    private String fxas;           // 고정자산
    private String totalAset;      // 자산총계
    private String flowLblt;       // 유동부채
    private String fixLblt;        // 고정부채
    private String totalLblt;      // 부채총계
    private String cpfn;           // 자본금
    private String cfpSurp;        // 자본 잉여금
    private String prfiSurp;       // 이익 잉여금
    private String totalCptl;      // 자본총계
}