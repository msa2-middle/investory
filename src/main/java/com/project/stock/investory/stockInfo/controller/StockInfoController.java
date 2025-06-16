package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.stock.investory.stockInfo.dto.*;
import com.project.stock.investory.stockInfo.service.StockInfoService;

import java.util.List;


@RestController
@RequestMapping("/stock/{mkscShrnIscd}/analytics")
public class StockInfoController {
    private StockInfoService stockInfoService ;

    @Autowired
    public StockInfoController(StockInfoService kisService) {
        this.stockInfoService = kisService;
    }


    @GetMapping("/productInfo")
    public ResponseEntity<ProductBasicDTO> getProductInfo(@PathVariable String mkscShrnIscd) {

       ProductBasicDTO dto = stockInfoService.getProductBasic(mkscShrnIscd);
       return ResponseEntity.ok(dto);
    }

    @GetMapping("/stock-info")
    public ResponseEntity<StockBasicDTO> getStockInfo(@PathVariable String mkscShrnIscd) {
        StockBasicDTO dto = stockInfoService.getStockBasic(mkscShrnIscd);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/balance-sheet")
    public ResponseEntity<List<BalanceSheetDTO>> getBalanceSheet(@PathVariable String mkscShrnIscd) {
        List<BalanceSheetDTO> dtoList = stockInfoService.getBalanceSheet(mkscShrnIscd);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/income-statement")
    public ResponseEntity<List<IncomeStatementDTO>> getIncomeStatement(@PathVariable String mkscShrnIscd) {
        List<IncomeStatementDTO> dtoList = stockInfoService.getIncomeStatement(mkscShrnIscd);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/financial-ratio")
    public ResponseEntity<List<FinancialRatioDTO>> getFinancialRatio(@PathVariable String mkscShrnIscd) {
        List<FinancialRatioDTO> dtoList = stockInfoService.getFinancialRatio(mkscShrnIscd);
        return ResponseEntity.ok(dtoList);
    }


    @GetMapping("/profitability-ratio")
    public ResponseEntity<List<ProfitRatioDTO>> getProfitRatio(@PathVariable String mkscShrnIscd) {
        List<ProfitRatioDTO> dtoList = stockInfoService.getProfitRatio(mkscShrnIscd);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/stability-ratio")
    public ResponseEntity<List<StabilityRatioDTO>> getStabilityRatio(@PathVariable String mkscShrnIscd) {
        List<StabilityRatioDTO> dtoList = stockInfoService.getStabilityRatio(mkscShrnIscd);
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/growth-ratio")
    public ResponseEntity<List<GrowthRatioDTO>> getGrowthRatio(@PathVariable String mkscShrnIscd) {
        List<GrowthRatioDTO> dtoList = stockInfoService.getGrowthRatio(mkscShrnIscd);
        return ResponseEntity.ok(dtoList);
    }


}
