package com.project.stock.investory.mainData.service;

import com.project.stock.investory.mainData.dto.RankDto;
import com.project.stock.investory.mainData.dto.StockPriceHistoryDto;
import com.project.stock.investory.mainData.dto.StockPriceHistoryResponseDto;
import com.project.stock.investory.mainData.entity.StockPriceHistory;
import com.project.stock.investory.mainData.repository.StockPriceHistoryRepository;
import com.project.stock.investory.stockInfo.service.StockSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StockPriceSaveService {

    private final StockPriceHistoryRepository stockPriceHistoryRepository;
    private final StockPriceHistoryService stockPriceHistoryService;
    private final RankService rankService;
    private final StockSaveService stockSaveService;

    @Autowired
    public StockPriceSaveService(StockPriceHistoryRepository stockPriceHistoryRepository,
                                 StockPriceHistoryService stockPriceHistoryService,
                                 RankService rankService,
                                 StockSaveService stockSaveService) {
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
        this.stockPriceHistoryService = stockPriceHistoryService;
        this.rankService = rankService;
        this.stockSaveService = stockSaveService;
    }


    // 특정 티커 주가 데이터 get
    public List<StockPriceHistoryResponseDto> getStockHistoryByTicker(String ticker) {
        // Repository 메서드 호출
        List<StockPriceHistory> entities = stockPriceHistoryRepository.findByStockIdOrderByTradeDateDesc(ticker);

        // 엔티티 리스트를 DTO 리스트로 변환하여 반환
        return entities.stream()
                .map(StockPriceHistoryResponseDto::fromEntity) // StockPriceHistoryDto에 fromEntity 메서드가 있다고 가정
                .collect(Collectors.toList());
    }


    /**
     * [saveAll]
     * 1. 시가총액 기준 티커별 DB에 saveAllTicker
     * 2. 주가 이력 DTO 리스트를 받아 DB에 saveAll
     */

    // 1. 시가총액 기준 티커별 DB에 saveAllTicker
    public void saveAllTicker(String period) {
        List<RankDto> rankData = rankService.getRankData("5").block();
        int i = 0;
        long start = System.currentTimeMillis();
        if (rankData != null) {
            for (RankDto dto : rankData) {

                String stockId = dto.getCode();
                i++;
                log.info("번호: {}, 종목: {}, 기준일자: {}", i, stockId, period);


                saveAll(stockId, period);

                // sleep
                try {
                    Thread.sleep(1234);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        } // end if

        long end = System.currentTimeMillis();
        log.info("elapsed all time(ms): {}", (end - start));
    }


    // 2. 주가 이력 DTO 리스트를 받아 DB에 saveAll - Weelky 데이터
    @Transactional
    public void saveAll(String stockId, String period) {

        long start = System.currentTimeMillis();

        // API 조회를 시작할 현재 기간 (초기값은 initialPeriod)
        String currentApiPeriod = period;

        // 목표로 하는 최종 날짜 (2022년 1월 1일)
        // LocalDate.of(YYYY, MM, DD) 형태로 직접 LocalDate 객체를 생성합니다.
        final LocalDate LAST_TARGET_DATE = LocalDate.of(2000, 1, 1);

        boolean hasMoreDataFromApi = true; // API에서 더 가져올 데이터가 있는지 여부

        do {
            // 1. API에서 데이터 받아오기
            List<StockPriceHistoryDto> dtoList;

            try {
                // 기간별 데이터 조회
                log.info("종목: {}, 기준일자: {}", stockId, currentApiPeriod);

                // weekly로 저장
                String periodDiv = "W";
                dtoList = stockPriceHistoryService.getStockPriceHistory(stockId, currentApiPeriod, periodDiv);

            } catch (Exception e) {
                log.error("API 호출 중 오류 발생 (종목: {}, 기간: {}): {}", stockId, currentApiPeriod, e.getMessage(), e);
                hasMoreDataFromApi = false;
                throw new RuntimeException("API 데이터 조회 실패", e);
            }

            if (dtoList == null || dtoList.isEmpty()) {
                log.info("API로부터 더 이상 받아올 데이터가 없습니다. 저장 작업을 종료합니다. 종목: {}, 마지막 조회 기간: {}", stockId, currentApiPeriod);
                hasMoreDataFromApi = false;
                continue;
            }

            // 2. DTO를 Entity로 변환
            List<StockPriceHistory> entitiesToSave = dtoList.stream()
                    .map(dto -> dto.toEntity(stockId))
                    .collect(Collectors.toList());

            // 3. saveAll
            try {
                stockPriceHistoryRepository.saveAll(entitiesToSave);
                System.out.println("총 " + entitiesToSave.size() + "개의 주가 이력 데이터 저장 완료.");
            } catch (Exception e) {
                System.err.println("주가 이력 데이터 저장 중 오류 발생: " + e.getMessage());
                e.printStackTrace(); // 상세한 스택 트레이스 출력
                // 필요하다면 예외를 다시 던지거나 특정 예외로 변환
                throw new RuntimeException("데이터베이스 저장 실패", e);
            }

            // 4. 다음 기간 계산 (가장 오래된 날짜 - 1일)
            // DB에서 stockId에 해당하는 가장 오래된 거래일 조회
            String oldestTradeDateStr = stockPriceHistoryRepository.findOldestTradeDateByStockId(stockId);

            if (oldestTradeDateStr != null) {
                // 문자열 → LocalDate 변환 (형식: "yyyyMMdd")
                LocalDate oldestDateInDb = LocalDate.parse(
                        oldestTradeDateStr,
                        DateTimeFormatter.BASIC_ISO_DATE // "yyyyMMdd" 형식 지원
                );

                // 다음 기간 계산: 가장 오래된 거래일 - 1일
                LocalDate nextPeriodDate = oldestDateInDb.minusDays(1);
                currentApiPeriod = nextPeriodDate.format(DateTimeFormatter.BASIC_ISO_DATE);

                // 종료 조건: 계산된 날짜가 목표일 이전인 경우
                if (nextPeriodDate.isBefore(LAST_TARGET_DATE)) {
                    hasMoreDataFromApi = false;
                }
            } else {
                // 조회 결과가 null인 경우 (저장된 데이터 없음)
                log.error("DB에서 가장 오래된 거래일 조회 실패. stockId: {}", stockId);
                hasMoreDataFromApi = false;
            }

            long end = System.currentTimeMillis();
            log.info("elapsed time(ms): {}", (end - start));

        } while (hasMoreDataFromApi);
    }


    /**
     * Daily 데이터 Save
     * 1. 한 종목 Daily 데이터 저장 - 일주일치
     * 2. 시가총액 종목들 Daily Data(당일 데이터) 저장
     */

    // 1.한 종목 Daily 데이터 저장 - 일주일치 - stock_id, trade_date unique적용
    @Transactional
    public void saveDailyPrice(String stockId) {

        // 오늘날짜기준 설정
        final String todayStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        // 1. API에서 데이터 받아오기
        List<StockPriceHistoryDto> dtoList;
        try {
            // Daily 저장
            String periodDiv = "D";
            dtoList = stockPriceHistoryService.getStockPriceHistory(stockId, todayStr, periodDiv);
        } catch (Exception e) {
            log.error("API 호출 중 오류 발생 (종목: {}, 기간: {}): {}", stockId, todayStr, e.getMessage(), e);
            dtoList = Collections.emptyList();
        }

        // 2. DTO를 Entity로 변환
        // -> 일주일치로 7개만 선택 (dtoList가 7개 미만이면 전체 반환)
        List<StockPriceHistoryDto> dtoList_week = dtoList.stream()
                .limit(7)
                .collect(Collectors.toList());

        List<StockPriceHistory> entitiesToSave = dtoList_week.stream()
                .map(dto -> dto.toEntity(stockId))
                .collect(Collectors.toList());

        for (StockPriceHistory entity : entitiesToSave) {
            long exist = stockPriceHistoryRepository.countByStockIdAndTradeDateNative(
                    entity.getStockId(), entity.getTradeDate());

            if (!(exist > 0)) {
                stockPriceHistoryRepository.save(entity);
            } else {
                log.info("중복 데이터 생략: stock_id={}, trade_date={}", entity.getStockId(), entity.getTradeDate());
            }
        }

    }

    // 2. 시가총액 종목들 Daily Data(당일 데이터) 저장 - stock_id, trade_date unique적용
    public void saveDailyPriceTicker() {
        List<RankDto> rankData = rankService.getRankData("5").block();
        int i = 0;
        long start = System.currentTimeMillis();
        if (rankData != null) {
            for (RankDto dto : rankData) {
                // 종목 코드
                String stockId = dto.getCode();
                i++;
                log.info("번호: {}, 종목: {}, 기준일자: {}", i, stockId);

                // 최근 일주일치 daily 가격 저장
                saveDailyPrice(stockId);

                // sleep
                try {
                    Thread.sleep(1234);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        } // end if

        long end = System.currentTimeMillis();
        log.info("elapsed all time(ms): {}", (end - start));
    }
}


