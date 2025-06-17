///*
//package com.project.stock.investory.config;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.socket.client.WebSocketClient;
//import org.springframework.web.socket.client.WebSocketConnectionManager;
//import org.springframework.web.socket.client.standard.StandardWebSocketClient;
//
//@Configuration
//public class WebSocketClientConfig {
//
////    private static final String approvalKey = "eaf82f2d-ed7b-422c-bad1-6582b77d636c";
//    private static final String trKey = "005930"; // 예: 삼성전자
//
//    @PostConstruct
//    public void connectToKisWebSocket() {
//        WebSocketClient client = new StandardWebSocketClient();
//
//        WebSocketConnectionManager manager = new WebSocketConnectionManager(
//                client,
//                new PriceStockWebSocketHandler(approvalKey, trKey),
//                "wss://openapi.koreainvestment.com:9443/websocket"
//        );
//
//        manager.setAutoStartup(true); // Spring 시작과 함께 자동 연결
//        manager.start();
//    }
//}*/
