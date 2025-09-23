package com.project.stock.investory.mainData.controller;

import com.project.stock.investory.mainData.dto.StockPriceDto;
import com.project.stock.investory.mainData.dto.StockPriceHistoryDto;
import com.project.stock.investory.mainData.dto.StockPriceHistoryResponseDto;
import com.project.stock.investory.mainData.service.StockPriceHistoryService;
import com.project.stock.investory.mainData.service.StockPriceSaveService;
import com.project.stock.investory.mainData.service.StockPriceService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // get 하루 실시간 가격
    @GetMapping("/{stockId}/price")
    public StockPriceDto getPrice(@PathVariable String stockId) {
        Mono<StockPriceDto> response = stockPriceService.getStockPrice(stockId);

        return response.block();
    }


    /**
     * [price history 조회]
     * 1. DB의 stock price history 조회
     * 2. 100영업일 데이터 가져오기
     */
    @Operation(summary = "DB의 stock price history 조회")
    @GetMapping("/{stockId}/history")
    public ResponseEntity<List<StockPriceHistoryResponseDto>> getStockPriceHistoryByTicker(@PathVariable String stockId) {
        try {
            // 서비스 계층의 메서드를 호출하여 데이터 조회
            List<StockPriceHistoryResponseDto> history = stockPriceSaveService.getStockHistoryByTicker(stockId); // 서비스 메서드 호출

            // 데이터가 없는 경우 404 Not Found 반환
            if (history.isEmpty()) {
                // 로그를 추가하여 어떤 데이터가 없는지 추적하는 게 좋습니다.
                System.out.println("No stock price history found for stockId: " + stockId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 조회된 데이터를 200 OK와 함께 반환
            return new ResponseEntity<>(history, HttpStatus.OK);

        } catch (Exception e) {
            // 예외 발생 시 500 Internal Server Error 반환 및 에러 로깅
            System.err.println("Error fetching stock price history for stockId " + stockId + ": " + e.getMessage());
            e.printStackTrace(); // 디버깅을 위해 스택 트레이스 출력
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // get 특정 종목 100개 가격 데이터
    @Operation(summary = "get 특정 종목 100개 가격 데이터(Y, W, D")
    @GetMapping("/api/{stockId}/history")
    public List<StockPriceHistoryDto> getStockPriceHistory(
            @PathVariable String stockId,
            @RequestParam String period,
            @RequestParam String periodDiv
    ) {

        return stockPriceHistoryService.getStockPriceHistory(stockId, period, periodDiv);
    }


    /**
     * [save]
     * 1. 특정 종목 가격 데이터 조회 및 save"
     * 2. 특정 종목의 오늘 주가(Daily) 데이터 저장
     * 3. 여러 티커 가져와서 가격 데이터 저장
     * 4. 시가총액 종목들의 일주일치 Daily 데이터 저장
     */

    // 특정 종목 가격 데이터 조회 및 save
    @Operation(summary = "[TEST]특정 종목 가격 데이터 조회 및 save")
    @PostMapping("/save/history/{stockId}")
    public ResponseEntity<String> fetchAndSaveStockPriceHistory(
            @PathVariable String stockId,
            @RequestParam String period
    ) {
        System.out.println("API 호출 시작: stockId=" + stockId + ", period=" + period); // 요청 시작 로그

        try {
            stockPriceSaveService.saveAll(stockId, period);
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

    @Operation(summary = "[TEST]특정 종목의 오늘 주가(Daily) 데이터 저장")
    @PostMapping("/save/daily/{stockId}")
    public ResponseEntity<String> saveDailyPrice(@PathVariable String stockId) {
        try {
            stockPriceSaveService.saveDailyPrice(stockId);
            return ResponseEntity.ok("주가 이력 데이터 저장 완료: " + stockId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("저장 중 오류 발생: " + e.getMessage());
        }
    }

    // 여러 티커 가져와서 가격 데이터 저장
    @Operation(summary = "[TEST]시가총액 100종목 가격 추이 데이터 저장(stock_id, trade_date unique적용)")
    @PostMapping("/save/history")
    public String saveAllTicker(@RequestParam String period) {
        try {
            stockPriceSaveService.saveAllTicker(period);
            return "모든 종목 데이터 저장 완료";
        } catch (Exception e) {
            e.printStackTrace();
            return "저장 실패: " + e.getMessage();
        }
    }

    @Operation(summary = "[TEST]시가총액 100종목들의 일주일치 Daily 데이터 저장(stock_id, trade_date unique적용)")
    @PostMapping("/save/daily/ticker")
    public ResponseEntity<String> saveDailyPriceTicker() {
        try {
            stockPriceSaveService.saveDailyPriceTicker();
            return ResponseEntity.ok("시가총액 종목들의 Daily 데이터 저장 완료");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("저장 중 오류 발생: " + e.getMessage());
        }
    }

}
