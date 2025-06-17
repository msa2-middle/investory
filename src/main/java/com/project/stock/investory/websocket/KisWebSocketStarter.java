package com.project.stock.investory.websocket;

import jakarta.annotation.PostConstruct;
import org.java_websocket.client.WebSocketClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class KisWebSocketStarter {
    @PostConstruct  // Spring Boot 시작 시 자동 실행
    public void start() throws Exception {
        URI uri = new URI("wss://ops.koreainvestment.com:9443/WebSocket");
        WebSocketClient client = new KisWebSocketClient(uri);
        client.connect();
    }
}
