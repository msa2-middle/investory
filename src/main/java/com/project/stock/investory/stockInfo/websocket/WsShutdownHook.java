package com.project.stock.investory.stockInfo.websocket;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WsShutdownHook {

    private final KisWebSocketClient kis;

    @PreDestroy                // 🎯 스프링 컨텍스트 종료 직전 자동 호출
    public void shutdown() {
        kis.disconnect();
    }
}