package com.project.stock.investory.stockInfo.websocket;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WsShutdownHook {
    private final KisWebSocketClient kisClient;

    @PreDestroy
    public void shutdown() {
        log.info("애플리케이션 종료 - KIS WebSocket 연결 해제");
        kisClient.disconnect();
    }
}