package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.dto.*;
import com.project.stock.investory.stockInfo.service.StockWebSocketService;
import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.stock.investory.stockInfo.service.StockInfoService;

import java.util.List;


@RestController
@RequestMapping("/stock/{stockId}/analytics")
@Slf4j
public class StockInfoController {
    private StockInfoService stockInfoService;
    private StockWebSocketService stockWebSocketService;

    @Autowired
    public StockInfoController(StockInfoService kisService, StockWebSocketService stockWebSocketService) {
        this.stockInfoService = kisService;
        this.stockWebSocketService = stockWebSocketService;
    }


    @GetMapping("/productInfo")
    public ResponseEntity<ProductBasicDTO> getProductInfo(@PathVariable String stockId) {

        log.info("[INFO] 요청 받은 stockId = " + stockId);

        // WebSocket 연결 및 구독 보장
        stockWebSocketService.ensureConnectedAndSubscribed(stockId);

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
