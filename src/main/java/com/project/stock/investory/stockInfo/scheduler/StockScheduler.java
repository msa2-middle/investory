package com.project.stock.investory.stockInfo.scheduler;

import com.project.stock.investory.stockInfo.service.StockSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockScheduler {

    @Autowired
    private final StockSaveService stockSaveService;

    // @Scheduled(cron = "*/30 * * * * *", zone = "Asia/Seoul") // 테스트용
    // 매일 오전 9시 디비에 데이터 저장.
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")  //
    public void saveDaily(){
        stockSaveService.saveVolumeRank();
        stockSaveService.saveTradingValueRank();
        stockSaveService.savePriceDownRank();
        stockSaveService.savePriceUpRank();
        stockSaveService.saveTopMarket();
    }
}
