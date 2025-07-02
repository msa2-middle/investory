package com.project.stock.investory.stockAlertSetting.event;

import com.project.stock.investory.stockAlertSetting.processor.KisWebSocketClientAlarm;
import com.project.stock.investory.stockAlertSetting.processor.StockPriceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertEventListener {

    private final KisWebSocketClientAlarm webSocketClient;
    private final StockPriceProcessor stockPriceProcessor;

    @EventListener
    @Async
    public void handleStockAlertEvent(StockAlertEvent event) {
        try {
            switch (event.getAction()) {
                case "ADD" -> {
                    log.info("WebSocket 구독 추가: {}", event.getStockCode());
                    webSocketClient.addSubscription(event.getStockCode());
                }
                case "REMOVE" -> {
                    log.info("WebSocket 구독 해제: {}", event.getStockCode());
                    webSocketClient.removeSubscription(event.getStockCode());
                }
                case "UPDATE" -> {
                    log.info("WebSocket 구독 업데이트: {}", event.getStockCode());
                    // 업데이트의 경우 refresh로 처리
                    webSocketClient.refreshSubscriptions();
                }
                case "REFRESH" -> {
                    log.info("WebSocket 구독 전체 새로고침");
                    webSocketClient.refreshSubscriptions();
                }
                default -> log.warn("알 수 없는 액션: {}", event.getAction());
            }
        } catch (Exception e) {
            log.error("StockAlertEvent 처리 중 오류 발생: action={}, stockCode={}",
                    event.getAction(), event.getStockCode(), e);
        }
    }

    @EventListener
    @Async
    public void handleStockPriceEvent(StockPriceEvent event) {
        try {
            log.debug("주가 이벤트 처리: 종목={}, 가격={}", event.getStockCode(), event.getCurrentPrice());
            // 🔥 StockPriceProcessor로 주가 데이터 전달
            stockPriceProcessor.process(event.getStockCode(), event.getCurrentPrice());
        } catch (Exception e) {
            log.error("StockPriceEvent 처리 중 오류 발생: 종목={}, 가격={}",
                    event.getStockCode(), event.getCurrentPrice(), e);
        }
    }
}