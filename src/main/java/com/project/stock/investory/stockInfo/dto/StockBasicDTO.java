package com.project.stock.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 주식 기본 조회
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StockBasicDTO {
    private String pdno;                    // 종목코드
    private String prdtName;                // 종목명
    private String prdtEngName;             // 종목 영문명
    private String mketIdCd;                // 시장구분코드
    private String lstgStqt;                // 상장주식수
    private String cpta;                    // 자본금
    private String papr;                    // 액면가
    private String thdtClpr;                // 당일종가
    private String bfdyClpr;                // 전일종가
    private String clprChngDt;              // 종가 변경일자
    private String kospi200ItemYn;          // 코스피200 여부
    private String stdIdstClsfCdName;       // 산업분류 (소분류)
    private String idxBztpLclsCdName;       // 산업분류 (대분류)
    private String admnItemYn;              // 관리종목 여부
    private String trStopYn;                // 거래정지 여부

}