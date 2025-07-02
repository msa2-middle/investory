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
public class FixedIncomeDto {
    // 채권 코드로, 특정 채권을 식별하는 고유한 코드입니다.
    @JsonProperty("bcdt_code")
    private String bcdtCode;

    // 채권의 한글명으로, 채권의 이름이나 명칭을 나타냅니다.
    @JsonProperty("hts_kor_isnm")
    private String korName;

    // 현재 금리 또는 가격을 나타내며, 채권의 최신 수익률이나 시장 가격을 의미합니다.
    @JsonProperty("bond_mnrt_prpr")
    private String currentYield;

    // 전일 대비 부호로, 채권 금리나 가격이 상승(2), 하락(3), 보합(5) 중 어떤 상태인지를 나타냅니다.
    @JsonProperty("prdy_vrss_sign")
    private String prdyVrssSign;

    // 전일 대비 변동폭으로, 금리나 가격이 전일 대비 얼마나 변했는지를 수치로 나타냅니다.
    @JsonProperty("bond_mnrt_prdy_vrss")
    private String prdyVrss;

    /**
     * 전일 대비 등락률로,
     * output1에서는 'prdy_ctrt',
     * output2에서는 'bstp_nmix_prdy_ctrt' 필드에 해당하며,
     * 금리나 가격의 변동 비율을 백분율로 나타냅니다.
     */
    //output1 'prdy_ctrt',
    @JsonProperty("prdy_ctrt")
    private String changeRate1;

    //output2 'bstp_nmix_prdy_ctrt'
    @JsonProperty("bstp_nmix_prdy_ctrt")
    private String changeRate2;

    // 기준일로, 데이터가 측정된 날짜를 'YYYYMMDD' 형식의 문자열로 나타냅니다.
    @JsonProperty("stck_bsop_date")
    private String baseDate;

}

