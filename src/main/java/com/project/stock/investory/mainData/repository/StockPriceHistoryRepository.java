package com.project.stock.investory.mainData.repository;


import com.project.stock.investory.mainData.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
}