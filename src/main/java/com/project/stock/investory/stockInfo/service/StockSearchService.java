package com.project.stock.investory.stockInfo.service;

import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchService {

    private final StockRepository stockRepository;

    public List<Stock> searchStocks(String keyword) {
        return stockRepository.findByStockIdContainingIgnoreCaseOrStockNameContainingIgnoreCase(
                keyword, keyword
        );
    }
}