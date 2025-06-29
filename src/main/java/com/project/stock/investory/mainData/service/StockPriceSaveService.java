package com.project.stock.investory.mainData.service;

import com.project.stock.investory.mainData.dto.StockPriceHistoryDto;
import com.project.stock.investory.mainData.entity.StockPriceHistory;
import com.project.stock.investory.mainData.repository.StockPriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockPriceSaveService {

    private final StockPriceHistoryRepository stockPriceHistoryRepository;

    @Autowired
    public StockPriceSaveService(StockPriceHistoryRepository stockPriceHistoryRepository) {
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
    }

    /**
     * 주가 이력 DTO 리스트를 받아 DB에 저장
     *
     * @param dtoList 주가 이력 DTO 리스트
     * @param stockId 저장할 종목코드 (외래키)
     */
    @Transactional // 모든 저장이 하나의 트랜잭션으로 묶이도록 합니다. (중요!)
    public void saveAll(List<StockPriceHistoryDto> dtoList, String stockId) {
        if (dtoList == null || dtoList.isEmpty()) {
            System.out.println("저장할 주가 이력 데이터가 없습니다.");
            return;
        }

        List<StockPriceHistory> entitiesToSave = dtoList.stream()
                .map(dto -> convertToEntity(dto, stockId)) // DTO를 Entity로 변환
                .collect(java.util.stream.Collectors.toList());

        try {
            stockPriceHistoryRepository.saveAll(entitiesToSave);
            System.out.println("총 " + entitiesToSave.size() + "개의 주가 이력 데이터 저장 완료.");
        } catch (Exception e) {
            System.err.println("주가 이력 데이터 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace(); // 상세한 스택 트레이스 출력
            // 필요하다면 예외를 다시 던지거나 특정 예외로 변환
            throw new RuntimeException("데이터베이스 저장 실패", e);
        }
    }

    /**
     * StockPriceHistoryDto를 StockPriceHistory Entity로 변환
     */
    private StockPriceHistory convertToEntity(StockPriceHistoryDto dto, String stockId) {
        // 날짜 형식 지정: "YYYYMMDD"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        return StockPriceHistory.builder()
                .stockId(stockId) // 컨트롤러에서 받은 stockId 사용
                .tradeDate(LocalDate.parse(dto.getStckBsopDate(), formatter)) // String -> LocalDate 파싱
                // 가격 정보는 String으로 오므로 Integer로 변환, 실패할 경우 0 또는 null 처리
                .openPrice(parseInteger(dto.getStckOprc()))
                .closePrice(parseInteger(dto.getStckClpr()))
                .highPrice(parseInteger(dto.getStckHgpr()))
                .lowPrice(parseInteger(dto.getStckLwpr()))
                // 거래량은 Long으로 변환
                .volume(parseLong(dto.getAcmlVol()))
                .build();
    }

    // String을 Integer로 안전하게 파싱하는 헬퍼 메서드
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null; // 또는 0 (요구사항에 따라)
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Integer 파싱 오류: " + value);
            return null; // 또는 0
        }
    }

    // String을 Long으로 안전하게 파싱하는 헬퍼 메서드
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null; // 또는 0L (요구사항에 따라)
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Long 파싱 오류: " + value);
            return null; // 또는 0L
        }
    }
}