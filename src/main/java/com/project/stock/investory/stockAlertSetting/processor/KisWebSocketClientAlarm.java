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
        System.out.println("[ALARM-OPEN] 알람 WebSocket 연결됨");

        List<String> stockCodes = stockRepository.findAllStockCodes();

        // 승인 요청
        session.getAsyncRemote().sendText(approvalJson());

        // 1초 후 종목별로 subscribe 전송
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            for (String code : stockCodes) {
                if (this.session != null && this.session.isOpen()) {
                    String subscribeMsg = subscribeJson(code);
                    this.session.getAsyncRemote().sendText(subscribeMsg);
                    System.out.println("[ALARM-SUBSCRIBE] 구독 요청 전송: " + code);
                } else {
                    System.out.println("[ALARM-WARN] 세션이 닫혀 있어 subscribe 실패");
                }
            }
        }, 1, TimeUnit.SECONDS);

        startHeartbeat();
    }

    @OnMessage
    public void onMessage(String message) {
//        System.out.println("[RECEIVED] " + message);
        System.out.println("[ALARM-RECEIVED- 민희가 받는 데이터] " + message);


        try {
            // JSON 메시지일 경우 pass
            if (message.trim().startsWith("{")) {
                System.out.println("[ALARM-INFO] JSON 메시지 수신 (승인 관련)");
                return;
            }

            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                System.out.println("[ALARM-WARN] 메시지 형식 오류 - 파트 수: " + parts.length);
                return;
            }

            String trId = parts[1];
            String seq = parts[2];
            String payload = parts[3];

            System.out.println("[ALARM-DEBUG] tr_id: " + trId + ", seq: " + seq);

            // 호가 데이터 처리
            if (!"H0STASP0".equals(trId)) {
                System.out.println("[ALARM-WARN] 예상하지 못한 tr_id: " + trId);
                return;
            }

            String[] fields = payload.split("\\^");
            System.out.println("[ALARM-DEBUG] 파싱된 필드 수: " + fields.length);

            // 필드 수 체크 (호가 데이터는 보통 50개 이상)
            if (fields.length < 23) {
                System.out.println("[ALARM-WARN] 호가 데이터 필드 수 부족: " + fields.length);
                return;
            }

            // 🔥 호가 데이터 구조에 맞게 파싱
            String stockCode = fields[0];
            String time = fields[1];
            // fields[2] = 구분값 (0) - 무시

            // 🔥 호가 정보 파싱
            int askPrice1 = Integer.parseInt(fields[3]);  // 매도1호가
            int askPrice2 = Integer.parseInt(fields[4]);  // 매도2호가
            // ... 매도3~10호가는 fields[5]~[12]

            int bidPrice1 = Integer.parseInt(fields[13]); // 매수1호가
            int bidPrice2 = Integer.parseInt(fields[14]); // 매수2호가
            // ... 매수3~10호가는 fields[15]~[22]

            // 🔥 현재가 추정 로직 (매도매수 전략에 맞게 선택)
            int estimatedCurrentPrice;

            // 옵션 1: 매수1호가를 현재가로 사용 (보수적)
            estimatedCurrentPrice = bidPrice1;

            // 옵션 2: 매도1호가를 현재가로 사용 (적극적)
            // estimatedCurrentPrice = askPrice1;

            // 옵션 3: 매도1호가와 매수1호가의 중간값
            // estimatedCurrentPrice = (askPrice1 + bidPrice1) / 2;

            // 데이터 구성
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stock_code", stockCode);
            data.put("time", time);
            data.put("ask_price_1", askPrice1);        // 매도1호가
            data.put("ask_price_2", askPrice2);        // 매도2호가
            data.put("bid_price_1", bidPrice1);        // 매수1호가
            data.put("bid_price_2", bidPrice2);        // 매수2호가
            data.put("current_price", estimatedCurrentPrice); // 🔥 추정 현재가
            data.put("spread", askPrice1 - bidPrice1); // 호가 스프레드

            // 전체 메시지 JSON 구성
            Map<String, Object> jsonMessage = new LinkedHashMap<>();
            jsonMessage.put("tr_id", trId);
            jsonMessage.put("seq", seq);
            jsonMessage.put("data", data);

            // JSON 출력
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMessage);
//            System.out.println(json);

            // 1. JSON 문자열 파싱
            JsonNode root = objectMapper.readTree(json);

            String stockCode = root.at("/data/stock_code").asText();
            int currentPrice = root.at("/data/current_price").asInt();

//            System.out.println(stockCode + "   " + currentPrice);
            System.out.println("[ALARM-JSON] " + json);

            // 🔥 알람 처리: 추정 현재가 사용
            System.out.println("[ALARM-PROCESS] 종목: " + stockCode +
                    ", 매도1호가: " + askPrice1 +
                    ", 매수1호가: " + bidPrice1 +
                    ", 추정현재가: " + estimatedCurrentPrice);

            stockPriceProcessor.process(stockCode, estimatedCurrentPrice);

        } catch (Exception e) {
            System.err.println("[ALARM-ERROR] 메시지 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[ALARM-CLOSE] 알람 세션 종료: " + reason);
        System.out.println("[ALARM-CLOSE CODE] " + reason.getCloseCode().getCode());
        stopHeartbeat();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[ALARM-ERROR] 알람 WebSocket 에러 발생: " + throwable.getMessage());
        throwable.printStackTrace();
        stopHeartbeat();
    }

    private void startHeartbeat() {
        System.out.println("[ALARM-INFO] 알람 하트비트 비활성화 - 서버 자체 PINGPONG 사용");
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
                        "tr_id": "H0STASP0",
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
                    "tr_id": "H0STASP0",
                    "tr_key": "%s"
                  },
                  "body": {
                    "input": {
                      "tr_id": "H0STASP0",
                      "tr_key": "%s"
                    }
                  }
                }
                """.formatted(approvalKey, stockCode, stockCode);
    }
}