package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.dto.*;
import com.project.stock.investory.stockInfo.service.StockInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/stock/{stockId}/analytics")
@Slf4j
public class StockInfoController {
    private StockInfoService stockInfoService;

    @Autowired
    public StockInfoController(StockInfoService kisService) {
        this.stockInfoService = kisService;
    }


    @GetMapping("/productInfo")
    public ResponseEntity<ProductBasicDTO> getProductInfo(@PathVariable String stockId) {

        ProductBasicDTO dto = stockInfoService.getProductBasic(stockId);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/stock-info")
    public ResponseEntity<StockBasicDTO> getStockInfo(@PathVariable String stockId) {
        StockBasicDTO dto = stockInfoService.getStockBasic(stockId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/balance-sheet")
    public ResponseEntity<List<BalanceSheetDTO>> getBalanceSheet(@PathVariable String stockId) {
        List<BalanceSheetDTO> dtoList = stockInfoService.getBalanceSheet(stockId);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/income-statement")
    public ResponseEntity<List<IncomeStatementDTO>> getIncomeStatement(@PathVariable String stockId) {
        List<IncomeStatementDTO> dtoList = stockInfoService.getIncomeStatement(stockId);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/financial-ratio")
    public ResponseEntity<List<FinancialRatioDTO>> getFinancialRatio(@PathVariable String stockId) {
        List<FinancialRatioDTO> dtoList = stockInfoService.getFinancialRatio(stockId);
        return ResponseEntity.ok(dtoList);
    }


    @GetMapping("/profitability-ratio")
    public ResponseEntity<List<ProfitRatioDTO>> getProfitRatio(@PathVariable String stockId) {
        List<ProfitRatioDTO> dtoList = stockInfoService.getProfitRatio(stockId);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/stability-ratio")
    public ResponseEntity<List<StabilityRatioDTO>> getStabilityRatio(@PathVariable String stockId) {
        List<StabilityRatioDTO> dtoList = stockInfoService.getStabilityRatio(stockId);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/growth-ratio")
    public ResponseEntity<List<GrowthRatioDTO>> getGrowthRatio(@PathVariable String stockId) {
        List<GrowthRatioDTO> dtoList = stockInfoService.getGrowthRatio(stockId);
        return ResponseEntity.ok(dtoList);
    }


}
