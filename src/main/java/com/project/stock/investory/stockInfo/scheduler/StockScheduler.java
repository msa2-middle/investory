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
    // 매일 메인 데이터의 종목list 9시, 12시, 15시 디비에 데이터 update
    @Scheduled(cron = "0 00 9 * * *", zone = "Asia/Seoul")  // 오전 9시 - 장시작
    @Scheduled(cron = "0 00 12 * * *", zone = "Asia/Seoul") // 정오
    @Scheduled(cron = "0 0 15 * * *", zone = "Asia/Seoul")  // 오후 3시 - 장마감
    public void saveDaily(){
        stockSaveService.saveVolumeRank();
        stockSaveService.saveTradingValueRank();
        stockSaveService.savePriceDownRank();
        stockSaveService.savePriceUpRank();
        stockSaveService.saveTopMarket();
    }
}
