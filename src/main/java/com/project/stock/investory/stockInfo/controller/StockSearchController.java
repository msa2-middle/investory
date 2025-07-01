package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.service.StockSearchService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockSearchController {

    private final StockSearchService stockService;

    @Operation(summary = "종목 검색 (자동완성용)")
    @GetMapping("/search")
    public ResponseEntity<List<Stock>> searchStocks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "100") int limit
    ) {
        List<Stock> stocks = stockService.searchStocks(keyword);
        return ResponseEntity.ok(stocks.stream().limit(limit).collect(Collectors.toList()));
    }
}
