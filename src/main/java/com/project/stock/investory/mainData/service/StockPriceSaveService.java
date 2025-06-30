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
        List<StockPriceHistory> entities = stockPriceHistoryRepository.findByStockId(ticker);

        // 엔티티 리스트를 DTO 리스트로 변환하여 반환
        return entities.stream()
                .map(StockPriceHistoryResponseDto::fromEntity) // StockPriceHistoryDto에 fromEntity 메서드가 있다고 가정
                .collect(Collectors.toList());
    }


    /**
     * 1. 시가총액 기준 티커별 DB에 saveAllTicker
     * 2. 주가 이력 DTO 리스트를 받아 DB에 saveAll
     */

    // 1. 시가총액 기준 티커별 DB에 saveAllTicker
    public void saveAllTicker(String period) {
        List<RankDto> rankData = rankService.getRankData("5").block();
        int i = 0;

        System.out.println("실행 start");
        long start = System.currentTimeMillis();
        if (rankData != null) {
            for (RankDto dto : rankData) {

                String stockId = dto.getCode();
                log.info("번호: {}, 종목: {}, 기준일자: {}", i, stockId, period);
                i++;

                saveAll(stockId, period);

                // sleep
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        } // end if

        long end = System.currentTimeMillis();
        System.out.println("실행 시간(ms): " + (end - start));
    }


    // 2. 주가 이력 DTO 리스트를 받아 DB에 saveAll
    @Transactional
    public void saveAll(String stockId, String period) {

        // API 조회를 시작할 현재 기간 (초기값은 initialPeriod)
        String currentApiPeriod = period;

        // 목표로 하는 최종 날짜 (2022년 1월 1일)
        // LocalDate.of(YYYY, MM, DD) 형태로 직접 LocalDate 객체를 생성합니다.
        final LocalDate LAST_TARGET_DATE = LocalDate.of(2022, 1, 1);

        boolean hasMoreDataFromApi = true; // API에서 더 가져올 데이터가 있는지 여부

        do {
            // 1. API에서 데이터 받아오기
            List<StockPriceHistoryDto> dtoList;

            try {
                // 기간별 데이터 조회
                log.info("종목: {}, 기준일자: {}", stockId, currentApiPeriod);
                dtoList = stockPriceHistoryService.getStockPriceHistory(stockId, currentApiPeriod);

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

        } while (hasMoreDataFromApi);
    }
}


