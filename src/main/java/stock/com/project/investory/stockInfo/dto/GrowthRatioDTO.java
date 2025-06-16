package stock.com.project.investory.stockInfo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 성장성비율
 */
@Getter
@Setter
@NoArgsConstructor
public class GrowthRatioDTO {
    private String stacYymm;         // 결산 년월
    private String grs;              // 매출액 증가율
    private String bsopPrfiInrt;     // 영업 이익 증가율
    private String equtInrt;         // 자기자본 증가율
    private String totlAsetInrt;     // 총자산 증가율
}