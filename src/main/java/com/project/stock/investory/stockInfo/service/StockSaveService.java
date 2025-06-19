package com.project.stock.investory.stockInfo.service;

import com.project.stock.investory.mainData.service.RankService;
import com.project.stock.investory.stockInfo.dto.StockApiResponseDTO;
import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSaveService {

    private final StockRepository stockRepository;
    private final RankService rankService;

    public void saveTopMarket() {
         // 1. 외부 API 호출
        List<StockApiResponseDTO> dtoList = rankService.getStockIdAndNameOnly("5").block();

        if(dtoList == null || dtoList.isEmpty()) return;

        // 2. DTO -> Entity로 변환
         List<Stock> stocks = dtoList.stream()
                 .map(dto -> Stock.builder()
                         .stockId(dto.getStockId())
                         .stockName(dto.getStockName())
                         .build())
                 .toList();


        // 3. 저장
        stockRepository.saveAll(stocks);
    }
}
