package com.project.stock.investory.mainData.repository;


import com.project.stock.investory.mainData.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {

    // 특정 주식 ID (ticker)에 해당하는 모든 주가 이력 데이터 조회
    List<StockPriceHistory> findByStockId(String stockId);

    // 특정 주식 ID (ticker)에 해당하는 모든 주가 이력 데이터를 최신 날짜 순으로 조회
    List<StockPriceHistory> findByStockIdOrderByTradeDateDesc(String stockId);

    // stockId와 tradeDate 함께 조회
//    boolean existsByStockIdAndTradeDate(String stockId, LocalDate tradeDate); -> 11g버전 사용 불가
    @Query(value = "SELECT COUNT(*) FROM stock_price " +
            "WHERE stock_id = :stockId AND trade_date = :tradeDate", nativeQuery = true)
    long countByStockIdAndTradeDateNative(@Param("stockId") String stockId,
                                          @Param("tradeDate") LocalDate tradeDate);

    // 가장 과거의 거래일 조회 (네이티브 쿼리 사용)
    @Query(value = """
            SELECT TO_CHAR(MIN(trade_date), 'YYYYMMDD')
            FROM stock_price
            WHERE stock_id = :stockId
            """, nativeQuery = true)
    String findOldestTradeDateByStockId(@Param("stockId") String stockId);


}