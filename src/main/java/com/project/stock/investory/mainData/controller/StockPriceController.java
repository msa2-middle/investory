package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.StockPriceDto;
import com.project.stock.investory.mainData.service.StockPriceService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class StockPriceController {
    private final StockPriceService stockPriceService;

    public StockPriceController(StockPriceService stockPriceService) {
        this.stockPriceService = stockPriceService;
    }

    @GetMapping("/stock/{iscd}/price")
    public StockPriceDto getPrice(@PathVariable String iscd, Model model) {
        Mono<StockPriceDto> response = stockPriceService.getStockPrice(iscd);

        return response.block();
    }

}
