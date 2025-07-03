package com.project.stock.investory.stockInfo.repository;

import com.project.stock.investory.stockInfo.model.Stock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository  extends CrudRepository<Stock, String> {

    @Query("SELECT s.stockId FROM Stock s")
    List<String> findAllStockCodes();

    // 종목코드(stockId) 또는 종목명(stockName)으로 검색 (대소문자 무시)
    List<Stock> findByStockIdContainingIgnoreCaseOrStockNameContainingIgnoreCase(
            String stockIdKeyword,
            String stockNameKeyword
    );

    @Query("SELECT COUNT(s) > 0 FROM Stock s WHERE s.stockId = :stockId")
    boolean existsByStockId(@Param("stockId") String stockId);
}
