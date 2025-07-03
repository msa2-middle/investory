package com.project.stock.investory.mainData.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "stock_price",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_trade_date", columnNames = {"stock_id", "trade_date"})
        }
)
public class StockPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_history_generator")
    @SequenceGenerator(name = "stock_history_generator", sequenceName = "stock_history", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    // Stock 테이블 외래키 (예: stock_id)
    @Column(name = "stock_id", nullable = false)
    private String stockId;

    // 거래일자 (DATE 타입)
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    // 시가
    @Column(name = "open_price")
    private Integer openPrice;

    // 종가
    @Column(name = "close_price")
    private Integer closePrice;

    // 고가
    @Column(name = "high_price")
    private Integer highPrice;

    // 저가
    @Column(name = "low_price")
    private Integer lowPrice;

    // 거래량
    @Column(name = "volume")
    private Long volume;
}
