package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.stockInfo.service.StockSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockSaveController {

    private final StockSaveService stockSaveService;

    public ResponseEntity<String> saveStockFromApi(){
        stockSaveService.saveTopMarket();
        return ResponseEntity.ok("Saved top market");
    }
}
