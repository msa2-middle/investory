package com.project.stock.investory.stockAlertSetting.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
@Component
@RequiredArgsConstructor
public class KisWebSocketClientAlarm {

    private final StockPriceProcessor stockPriceProcessor;
    private final StockRepository stockRepository;

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

        // todo: String code = 설정하기 db에서 가져오기
        // DB에서 종목코드 가져오기
        List<String> stockCodes = stockRepository.findAllStockCodes();
//        List<String> stockCodes = List.of("005930", "000660", "035420");  // 삼성전자, SK하이닉스, NAVER

        // 승인 요청
        // todo: approvalJson() 안에다가 code 넣어주기
        session.getAsyncRemote().sendText(approvalJson());

        // 1초 후 종목별로 subscribe 전송
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            for (String code : stockCodes) {
                if (this.session != null && this.session.isOpen()) {
                    String subscribeMsg = subscribeJson(code);
                    this.session.getAsyncRemote().sendText(subscribeMsg);
                    System.out.println("[SUBSCRIBE] 구독 요청 전송: " + subscribeMsg);
                } else {
                    System.out.println("[WARN] 세션이 닫혀 있어 subscribe 실패");
                }
            }
        }, 1, TimeUnit.SECONDS);

        startHeartbeat();
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("[RECEIVED] " + message);

        try {
            // 1. JSON 메시지일 경우 pass
            if (message.trim().startsWith("{")) return;

            String[] parts = message.split("\\|");
            if (parts.length < 4) return;

            String trId = parts[1];
            String seq = parts[2];
            String payload = parts[3];

            if (!"H0STCNT0".equals(trId)) return;


            String[] fields = payload.split("\\^");

            // 2. 각 필드에 이름 붙이기 (일부 주요 필드만 예시)
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stock_code", fields[0]);
            data.put("time", fields[1]);
            data.put("current_price", Integer.parseInt(fields[2]));
            data.put("sign", Integer.parseInt(fields[3]));
            data.put("change", Integer.parseInt(fields[4]));
            data.put("change_rate", Double.parseDouble(fields[5]));
            data.put("average_price", Double.parseDouble(fields[6]));
            data.put("high_price", Integer.parseInt(fields[7]));
            data.put("low_price", Integer.parseInt(fields[8]));
            data.put("bid_price", Integer.parseInt(fields[9]));
            data.put("ask_price", Integer.parseInt(fields[10]));
            data.put("base_price", Integer.parseInt(fields[11]));

            // 3. 전체 메시지 JSON 구성
            Map<String, Object> jsonMessage = new LinkedHashMap<>();
            jsonMessage.put("tr_id", trId);
            jsonMessage.put("seq", seq);
            jsonMessage.put("data", data);

            // 4. JSON 출력
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMessage);
            System.out.println(json);

            // 1. JSON 문자열 파싱
            JsonNode root = objectMapper.readTree(json);

            String stockCode = root.at("/data/stock_code").asText();
            int currentPrice = root.at("/data/current_price").asInt();

            System.out.println(stockCode + "   " + currentPrice);

            stockPriceProcessor.process(stockCode, currentPrice);


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