package com.project.stock.investory.stockInfo.repository;

import com.project.stock.investory.stockInfo.model.Stock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository  extends CrudRepository<Stock, String> {

    @Query("SELECT s.stockId FROM Stock s")
    List<String> findAllStockCodes();
}
