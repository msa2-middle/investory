//package com.project.stock.investory.config;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
//@Slf4j
//@RequiredArgsConstructor
//public class PriceStockWebSocketHandler extends TextWebSocketHandler {
//
//    private final String approvalKey;
//    private final String trKey;
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        log.info("✅ WebSocket 연결 성공");
//
//        String approvalMsg = """
//        {
//          "header": {
//            "approval_key": "%s",
//            "custtype": "P",
//            "tr_type": "1",
//            "content-type": "utf-8"
//          },
//          "body": {
//            "input": {}
//          }
//        }
//        """.formatted(approvalKey);
//
//        session.sendMessage(new TextMessage(approvalMsg));
//        log.info("📨 인증 메시지 전송");
//
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                String subscribeMsg = """
//                {
//                  "header": {
//                    "tr_id": "H0STASP0",
//                    "tr_key": "%s"
//                  }
//                }
//                """.formatted(trKey);
//
//                try {
//                    session.sendMessage(new TextMessage(subscribeMsg));
//                    log.info("📨 구독 메시지 전송");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 2000);
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
//        String payload = message.getPayload();
//        log.info("📥 실시간 시세 수신: {}", payload);
//    }
//}
