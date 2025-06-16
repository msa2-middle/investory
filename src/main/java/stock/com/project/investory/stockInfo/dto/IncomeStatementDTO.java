package stock.com.project.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 손익계산서
 */
@Getter
@Setter
@NoArgsConstructor
public class IncomeStatementDTO {
    private String stacYymm;       // 결산 년월
    private String saleAccount;    // 매출액
    private String saleCost;       // 매출 원가
    private String saleTotlPrfi;   // 매출 총 이익
    private String deprCost;       // 감가상각비
    private String sellMang;       // 판매 및 관리비
    private String bsopPrti;       // 영업 이익
    private String bsopNonErnn;    // 영업 외 수익
    private String bsopNonExpn;    // 영업 외 비용
    private String opPrfi;         // 경상 이익
    private String specPrfi;       // 특별 이익
    private String specLoss;       // 특별 손실
    private String thtrNtin;       // 당기순이익
}