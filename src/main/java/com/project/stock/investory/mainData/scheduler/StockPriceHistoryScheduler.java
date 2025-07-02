package com.project.stock.investory.mainData.scheduler;

import com.project.stock.investory.mainData.service.StockPriceSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockPriceHistoryScheduler {

    @Autowired
    private final StockPriceSaveService stockPriceSaveService;

    // 장 마감 후 15시 50분에 현재 가격 데이터 일괄 업데이트
    @Scheduled(cron = "00 50 15 * * *", zone = "Asia/Seoul")  // 장마감이후
    public void saveDaily() {
        stockPriceSaveService.saveDailyPriceTicker();

    }
}