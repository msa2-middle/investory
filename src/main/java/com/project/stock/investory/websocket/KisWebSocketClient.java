//package com.project.stock.investory.websocket;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.websocket.*;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//@ClientEndpoint
//@Component
//public class KisWebSocketClient {
//
//    private Session session;
//
//    @PostConstruct
//    public void connect() {
//        try {
//            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//            container.connectToServer(this, new URI("ws://ops.koreainvestment.com:21000/WebSocket"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @OnOpen
//    public void onOpen(Session session) {
//        this.session = session;
//        System.out.println("[OPEN] 연결됨");
//
//        session.getAsyncRemote().sendText(approvalJson());
//
//        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
//            if (this.session != null && this.session.isOpen()) {
//                this.session.getAsyncRemote().sendText(subscribeJson("005930"));
//            } else {
//                System.out.println("[WARN] 세션이 닫혀 있어 subscribe 실패");
//            }
//        }, 2, TimeUnit.SECONDS);
//
//        startHeartbeat(); // 💡 하트비트 스케줄 시작
//    }
//
//
//    @OnMessage
//    public void onMessage(String message) {
//        // 👇 여기서 콘솔에 찍힘
//        System.out.println("[RECEIVED] " + message);
//    }
//
//
//    @OnClose
//    public void onClose(Session session, CloseReason reason) {
//        System.out.println("[CLOSE] 세션 종료: " + reason);
//    }
//
//    @OnError
//    public void onError(Session session, Throwable throwable) {
//        System.err.println("[ERROR] 에러 발생: " + throwable.getMessage());
//        throwable.printStackTrace();
//    }
//
//    private void startHeartbeat() {
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
//            if (session != null && session.isOpen()) {
//                String now = java.time.LocalDateTime.now()
//                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//
//                String heartbeat = """
//            {
//              "header": {
//                "approval_key": "5bc10972-dec6-4c06-9f95-d7260421f81a",
//                "custtype": "P",
//                "tr_type": "1",
//                "content-type": "utf-8",
//                "tr_id": "PINGPONG",
//                "tr_key": "005930",
//                "datetime": "%s"
//              },
//              "body": {
//                "input": {}
//              }
//            }
//            """.formatted(now);
//
//                session.getAsyncRemote().sendText(heartbeat);
//                System.out.println("[PING] 하트비트 전송: " + now);
//            }
//        }, 10, 15, TimeUnit.SECONDS);
//    }
//
//    private String approvalJson() {
//        return """
//        {
//          "header": {
//            "approval_key": "5bc10972-dec6-4c06-9f95-d7260421f81a",
//            "custtype": "P",
//            "tr_type": "1",
//            "content-type": "utf-8"
//          },
//          "body": {
//            "input": {
//                "tr_id": "H0STCNT0",
//                "tr_key": "005930"
//                     }
//                }
//        }
//        """;
//    }
//
//    private String subscribeJson(String stockCode) {
//        return """
//    {
//      "header": {
//        "approval_key": "5bc10972-dec6-4c06-9f95-d7260421f81a",
//        "custtype": "P",
//        "tr_type": "1",
//        "content-type": "utf-8",
//        "tr_id": "H0STCNT0",
//        "tr_key": "%s"
//      },
//      "body": {
//        "input": {
//          "tr_id": "H0STCNT0",
//          "tr_key": "%s"
//        }
//      }
//    }
//    """.formatted(stockCode, stockCode);
//    }
//}
//
//

package com.project.stock.investory.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
@Component
public class KisWebSocketClient {

    private Session session;
    private ScheduledExecutorService heartbeatExecutor;

    @PostConstruct
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI("ws://ops.koreainvestment.com:21000/WebSocket"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[OPEN] 연결됨");

        // 승인 요청
        session.getAsyncRemote().sendText(approvalJson());

        // 구독 요청을 더 빨리 전송 (1초 후)
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            if (this.session != null && this.session.isOpen()) {
                String subscribeMsg = subscribeJson("005930");
                this.session.getAsyncRemote().sendText(subscribeMsg);
                System.out.println("[SUBSCRIBE] 구독 요청 전송: " + subscribeMsg);
            } else {
                System.out.println("[WARN] 세션이 닫혀 있어 subscribe 실패");
            }
        }, 1, TimeUnit.SECONDS);

        startHeartbeat();
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("[RECEIVED] " + message);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[CLOSE] 세션 종료: " + reason);
        stopHeartbeat();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[ERROR] 에러 발생: " + throwable.getMessage());
        throwable.printStackTrace();
        stopHeartbeat();
    }

    private void startHeartbeat() {
        // 하트비트를 비활성화 - 서버에서 자체적으로 PINGPONG 처리
        // heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        // heartbeatExecutor.scheduleAtFixedRate(() -> {
        //     if (session != null && session.isOpen()) {
        //         session.getAsyncRemote().sendText("PING");
        //         System.out.println("[PING] 하트비트 전송: PING");
        //     }
        // }, 30, 30, TimeUnit.SECONDS);
        System.out.println("[INFO] 하트비트 비활성화 - 서버 자체 PINGPONG 사용");
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
        }
    }

    private String approvalJson() {
        return """
        {
          "header": {
            "approval_key": "5bc10972-dec6-4c06-9f95-d7260421f81a",
            "custtype": "P",
            "tr_type": "1",
            "content-type": "utf-8"
          },
          "body": {
            "input": {
                "tr_id": "H0STCNT0",
                "tr_key": "005930"
            }
          }
        }
        """;
    }

    private String subscribeJson(String stockCode) {
        return """
        {
          "header": {
            "approval_key": "5bc10972-dec6-4c06-9f95-d7260421f81a",
            "custtype": "P",
            "tr_type": "1",
            "content-type": "utf-8",
            "tr_id": "H0STCNT0",
            "tr_key": "%s"
          },
          "body": {
            "input": {
              "tr_id": "H0STCNT0",
              "tr_key": "%s"
            }
          }
        }
        """.formatted(stockCode, stockCode);
    }
}