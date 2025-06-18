package com.project.stock.investory.stockInfo.service;

import com.project.stock.investory.stockInfo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockSaveService {
    private final StockRepository stockRepository;

    public void saveTopMarket() {
    }
}
