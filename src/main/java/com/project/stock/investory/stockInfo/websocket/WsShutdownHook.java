package com.project.stock.investory.stockInfo.websocket;

import com.project.stock.investory.stockInfo.service.StockWebSocketService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WsShutdownHook {
    private final KisWebSocketClient kisClient;
    private final StockWebSocketService stockWebSocketService;

    @PreDestroy
    public void shutdown() {
        log.info("애플리케이션 종료 - WebSocket 및 SSE 연결 해제");

        try {
            // SSE 연결 먼저 정리
            stockWebSocketService.shutdown();
        } catch (Exception e) {
            log.warn("SSE 연결 정리 중 오류", e);
        }

        try {
            // WebSocket 연결 정리
            kisClient.disconnect();
        } catch (Exception e) {
            log.warn("WebSocket 연결 정리 중 오류", e);
        }
    }
}