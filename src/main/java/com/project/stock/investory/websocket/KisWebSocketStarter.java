package com.project.stock.investory.websocket;

import jakarta.annotation.PostConstruct;
import org.java_websocket.client.WebSocketClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;


//        URI uri = new URI("wss://ops.koreainvestment.com:21000/WebSocket");
//        URI uri = new URI("wss://ops.koreainvestment.com:21000/tryitout/H0STASP0");
@Component
public class KisWebSocketStarter {
    @PostConstruct  // Spring Boot 시작 시 자동 실행
    public void start() throws Exception {
        System.out.println("🚀 WebSocketStarter 시작됨");

        new Thread(() -> {
            try {
//                URI uri = new URI("wss://ops.koreainvestment.com:21000/tryitout/H0STASP0");
                URI uri = new URI("wss://ops.koreainvestment.com:21000/WebSocket");
                WebSocketClient client = new KisWebSocketClient(uri);
                client.connectBlocking(); // 이건 새 쓰레드에서 수행되므로 blocking OK
                System.out.println("✅ WebSocket 연결됨");
            } catch (Exception e) {
                System.err.println("❌ WebSocket 연결 실패");
                e.printStackTrace();
            }
        }).start();

    }
}
