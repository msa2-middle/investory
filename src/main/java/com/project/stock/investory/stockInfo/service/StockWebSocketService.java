package com.project.stock.investory.stockInfo.service;

import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import org.springframework.stereotype.Service;

@Service
public class StockWebSocketService {

    private final KisWebSocketClient kisWebSocketClient;
    private final Object lock = new Object();

    public StockWebSocketService(KisWebSocketClient kisWebSocketClient) {
        this.kisWebSocketClient = kisWebSocketClient;
    }

    public void ensureConnectedAndSubscribed(String stockId) {
        synchronized (lock) {
            try {
                if (!kisWebSocketClient.isConnected()) {
                    System.out.println("[WS] 연결 안됨 → 연결 시도");
                    kisWebSocketClient.connect();
                    Thread.sleep(1000); // 연결 안정화 대기
                }
                kisWebSocketClient.subscribe(stockId);
            } catch (Exception e) {
                System.err.println("[WS ERROR] WebSocket 연결/구독 실패");
                e.printStackTrace();
            }
        }
    }
}
