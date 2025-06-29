package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.StockPriceDto;
import com.project.stock.investory.mainData.dto.StockPriceHistoryDto;
import com.project.stock.investory.mainData.service.StockPriceHistoryService;
import com.project.stock.investory.mainData.service.StockPriceSaveService;
import com.project.stock.investory.mainData.service.StockPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/stock")
@Slf4j
public class StockPriceController {
    private final StockPriceService stockPriceService;
    private final StockPriceHistoryService stockPriceHistoryService;
    private final StockPriceSaveService stockPriceSaveService;

    public StockPriceController(StockPriceService stockPriceService, StockPriceHistoryService stockPriceHistoryService, StockPriceSaveService stockPriceSaveService) {
        this.stockPriceService = stockPriceService;
        this.stockPriceHistoryService = stockPriceHistoryService;
        this.stockPriceSaveService = stockPriceSaveService;
    }

    // 하루 가격
    @GetMapping("/{stockId}/price")
    public StockPriceDto getPrice(@PathVariable String stockId, Model model) {
        Mono<StockPriceDto> response = stockPriceService.getStockPrice(stockId);

        return response.block();
    }


    /**
     * 주식 가격 이력 조회 API
     */
    // 30영업일 가격 가져오기
    @GetMapping("/{stockId}/history")
    public List<StockPriceHistoryDto> getStockPriceHistory(
            @PathVariable String stockId,
            @RequestParam String period
    ) {

        return stockPriceHistoryService.getStockPriceHistory(stockId, period);
    }


    // 가격 데이터 조회 및 저장 엔드포인트
    @PostMapping("/{stockId}/history/save")
    public ResponseEntity<String> fetchAndSaveStockPriceHistory(
                                                                 @PathVariable String stockId,
                                                                 @RequestParam String period
    ) {
        System.out.println("API 호출 시작: stockId=" + stockId + ", period=" + period); // 요청 시작 로그

        try {
            // 1. API에서 데이터 받아오기
            List<StockPriceHistoryDto> dtoList = stockPriceHistoryService.getStockPriceHistory(stockId, period);

            if (dtoList == null || dtoList.isEmpty()) {
                System.out.println("API로부터 받아온 데이터가 없거나 비어 있습니다. 저장 작업을 건너뜜.");
                return new ResponseEntity<>("데이터를 찾을 수 없거나 비어있어 저장하지 않았습니다.", HttpStatus.NOT_FOUND); // 404 응답
            }

            System.out.println("API로부터 " + dtoList.size() + "개의 데이터 수신: " + dtoList); // 수신 데이터 로그

            // 2. 저장 서비스 호출
            stockPriceSaveService.saveAll(dtoList, stockId);
            System.out.println("데이터베이스 저장 완료."); // 저장 완료 로그

            return new ResponseEntity<>("주가 이력 데이터 저장 성공", HttpStatus.OK); // 200 OK 응답

        } catch (RuntimeException e) { // 서비스 계층에서 던진 RuntimeException 캐치
            System.err.println("API 호출 또는 데이터 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 상세 스택 트레이스 출력 (디버깅용)
            return new ResponseEntity<>("주가 이력 데이터 저장 실패: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR); // 500 에러 응답
        } catch (Exception e) { // 그 외 예상치 못한 모든 예외 캐치
            System.err.println("예상치 못한 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("예상치 못한 서버 오류: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR); // 500 에러 응답
        }
    }

}
