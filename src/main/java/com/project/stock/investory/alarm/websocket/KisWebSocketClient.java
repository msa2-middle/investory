package com.project.stock.investory.alarm.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
@Component
@RequiredArgsConstructor
public class KisWebSocketClient {

    private final StockPriceProcessor stockPriceProcessor;

    // application.properties에서 주입
    @Value("${koreainvest.approval-key}")
    private String approvalKey;

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

        try {
            // JSON 파싱 → 종목코드, 현재가만 추출
            JsonNode root = new ObjectMapper().readTree(message);
            String trId = root.at("/header/tr_id").asText();

            if ("H0STCNT0".equals(trId)) {
                String stockCode = root.at("/body/output1/symb").asText();
                int currentPrice = root.at("/body/output1/stck_prpr").asInt();

                // 👉 데이터 처리 클래스에 위임
                stockPriceProcessor.process(stockCode, currentPrice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    //[CLOSE] 세션 종료: CloseReason: code [1006], reason [null]
    //[CLOSE CODE] 1006 --> 현재의 구성으로는 (비정상종료)강제종료 25/06/20/오후 7:30
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[CLOSE] 세션 종료: " + reason);
        System.out.println("[CLOSE CODE] " + reason.getCloseCode().getCode());
        stopHeartbeat();

//        // 장이 닫힌 상태에서도 재연결 시도 (단, 서버가 거부할 수 있음)
//        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
//            System.out.println("[RECONNECT] 5초 후 재연결 시도");
//            connect();
//        }, 5, TimeUnit.SECONDS);
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
            "approval_key": "%s",
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
        """.formatted(approvalKey);
    }

    private String subscribeJson(String stockCode) {
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
        """.formatted(approvalKey, stockCode, stockCode);
    }
}