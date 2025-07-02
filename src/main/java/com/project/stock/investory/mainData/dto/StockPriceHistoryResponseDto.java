package com.project.stock.investory.mainData.dto;

import com.project.stock.investory.mainData.entity.StockPriceHistory;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceHistoryResponseDto {

    private Long id;
    // 티커
    private String stockId;
    // 거래일
    private LocalDate tradeDate;
    // 시가
    private Integer openPrice;
    // 종가
    private Integer closePrice;
    // 고가
    private Integer highPrice;
    // 저가
    private Integer lowPrice;
    // 거래량
    private Long volume;

    /**
     * StockPriceHistory Entity 객체를 StockPriceHistoryResponseDto 객체로 변환
     * 이 메서드는 엔티티에서 필요한 필드들을 DTO로 복사하는 역할을 합니다.
     *
     * @param entity 변환할 StockPriceHistory 엔티티
     * @return 변환된 StockPriceHistoryResponseDto
     */
    public static StockPriceHistoryResponseDto fromEntity(StockPriceHistory entity) {
        if (entity == null) {
            return null; // 또는 빈 DTO를 반환하는 등의 예외 처리
        }
        return StockPriceHistoryResponseDto.builder()
                .id(entity.getId())
                .stockId(entity.getStockId())
                .tradeDate(entity.getTradeDate())
                .openPrice(entity.getOpenPrice())
                .closePrice(entity.getClosePrice())
                .highPrice(entity.getHighPrice())
                .lowPrice(entity.getLowPrice())
                .volume(entity.getVolume())
                .build();
    }

}
