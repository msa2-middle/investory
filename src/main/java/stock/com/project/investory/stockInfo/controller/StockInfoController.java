package stock.com.project.investory.stockInfo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import stock.com.project.investory.stockInfo.dto.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import stock.com.project.investory.stockInfo.service.StockInfoService;
import reactor.core.publisher.Mono;
import java.util.List;


@RestController
@RequestMapping("/stock/{mkscShrnIscd}/analytics")
public class StockInfoController {
    private StockInfoService kisService;

    @Autowired
    public StockInfoController(StockInfoService kisService) {
        this.kisService = kisService;
    }


    @GetMapping("/productInfo")
    public Mono<ProductBasicDTO> getProductInfo(@PathVariable String mkscShrnIscd) {

        return kisService.getProductBasic(mkscShrnIscd);
    }

    @GetMapping("/stock-info")
    public Mono<StockBasicDTO> getStockInfo(@PathVariable String mkscShrnIscd) {
        return kisService.getStockBasic(mkscShrnIscd);
    }

    @GetMapping("/balance-sheet")
    public Mono<List<BalanceSheetDTO>> getBalanceSheet(@PathVariable String mkscShrnIscd) {
        return kisService.getBalanceSheet(mkscShrnIscd);
    }

    @GetMapping("/income-statement")
    public Mono<List<IncomeStatementDTO>> getIncomeStatement(@PathVariable String mkscShrnIscd) {
        return kisService.getIncomeStatement(mkscShrnIscd);
    }

    @GetMapping("/financial-ratio")
    public Mono<List<FinancialRatioDTO>> getFinancialRatio(@PathVariable String mkscShrnIscd) {
        return kisService.getFinancialRatio(mkscShrnIscd);
    }


    @GetMapping("/profitablilty-ratio")
    public Mono<List<ProfitRatioDTO>> getProfitRatio(@PathVariable String mkscShrnIscd) {
        return kisService.getProfitRatio(mkscShrnIscd);
    }

    @GetMapping("/stability-ratio")
    public Mono<List<StabilityRatioDTO>> getStabilityRatio(@PathVariable String mkscShrnIscd) {
        return kisService.getStabilityRatio(mkscShrnIscd);
    }

    @GetMapping("/growth-ratio")
    public Mono<List<GrowthRatioDTO>> getGrowthRatio(@PathVariable String mkscShrnIscd) {
        return kisService.getGrowthRatio(mkscShrnIscd);
    }


}
