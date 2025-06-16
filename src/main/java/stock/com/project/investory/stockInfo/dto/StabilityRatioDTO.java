package stock.com.project.investory.stockInfo.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *  안정성비율
 */
@Getter
@NoArgsConstructor
@Setter
public class StabilityRatioDTO {
    private String stacYymm;     // 결산 년월
    private String lbltRate;     // 부채 비율
    private String bramDepn;     // 차입금 의존도
    private String crntRate;     // 유동 비율
    private String quckRate;     // 당좌 비율
}
