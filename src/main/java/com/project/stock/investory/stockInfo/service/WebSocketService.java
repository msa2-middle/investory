//package com.project.stock.investory.stockInfo.service;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//
//@Component
//public class WebSocketService {
//    private WebSocketClinet client;
//
//    @PostConstruct
//    public void startConnection() {
//        try {
//            if (client == null || !client.isOpen()) {
//                client = new WebSocketClinet(new URI("wss://ops.koreainvestment.com:9443/WebSocket"));
//                client.connect();
//            }
//        } catch (Exception e) {
//            System.err.println("🚨 WebSocket 연결 실패:");
//            e.printStackTrace();
//        }
//    }
//}
