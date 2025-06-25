package com.project.stock.investory.stockInfo.websocket;

import jakarta.websocket.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

@ClientEndpoint
@Component
public class KisWebSocketClient {

    private Session session;
    private ScheduledExecutorService heartbeatExecutor;

    @Value("${approval_key}")
    private String approvalKey;

    public boolean isConnected() {
        return session != null && session.isOpen();
    }


    public void connect() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, new URI("ws://ops.koreainvestment.com:21000/WebSocket"));
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[OPEN] 연결됨");

        // 승인 요청
        session.getAsyncRemote().sendText(approvalJson());

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

    public void subscribe(String stockId) {
        if (isConnected()) {
            String msg = subscribeJson(stockId);
            session.getAsyncRemote().sendText(msg);
            System.out.println("[SUBSCRIBE] 종목 구독 요청 전송: " + stockId);
        }
    }

    private void startHeartbeat() {
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
                    "approval_key": "%s",
                    "custtype": "P",
                    "tr_type": "1",
                    "content-type": "utf-8"
                  },
                  "body": {
                    "input": {
                      "tr_id": "H0STCNT0",
                      "tr_key": "000000"
                    }
                  }
                }
                """.formatted(approvalKey); // 고정 종목으로 승인 (트릭), 실제 구독은 별도로
    }

    private String subscribeJson(String stockId) {
        return """
                {
                  "header": {
                    "approval_key": "%s",
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
                """.formatted(approvalKey, stockId, stockId);
    }


}
