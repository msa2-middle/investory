package com.project.stock.investory.stockInfo.repository;

import com.project.stock.investory.stockInfo.model.Stock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository  extends CrudRepository<Stock, Long> {

}
