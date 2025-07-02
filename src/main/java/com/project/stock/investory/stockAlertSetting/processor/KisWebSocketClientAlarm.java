package com.project.stock.investory.stockAlertSetting.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockAlertSetting.event.StockPriceEvent;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import com.project.stock.investory.stockAlertSetting.repository.StockAlertSettingRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
@Component
@RequiredArgsConstructor
public class KisWebSocketClientAlarm {

    private final StockRepository stockRepository;
    private final StockAlertSettingRepository stockAlertSettingRepository;
    private final ApplicationEventPublisher eventPublisher; // 🔥 이벤트 발행자 추가
    private volatile boolean isApplicationShuttingDown = false;


    @Value("${koreainvest.approval-key}")
    private String approvalKey;

    private Session session;
    private ScheduledExecutorService heartbeatExecutor;

    // 현재 구독 중인 종목들을 추적
    private final Set<String> subscribedStocks = ConcurrentHashMap.newKeySet();

    // 구독 대기열 (연결 후 처리)
    private final Queue<String> pendingSubscriptions = new LinkedList<>();
    private final Queue<String> pendingUnsubscriptions = new LinkedList<>();

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

        // 승인 요청
        session.getAsyncRemote().sendText(approvalJson());

        // 1초 후 알람 설정된 종목들만 구독
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            subscribeToAlertStocks();
        }, 1, TimeUnit.SECONDS);

        startHeartbeat();
    }

    @OnMessage
    public void onMessage(String message) {
//        System.out.println("[ALARM-RECEIVED] " + message);

        if (isApplicationShuttingDown) {
            return; // 종료 중이면 처리 중단
        }


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

//            System.out.println("[ALARM-DEBUG] tr_id: " + trId + ", seq: " + seq);

            // 호가 데이터 처리
            if (!"H0STASP0".equals(trId)) {
                System.out.println("[ALARM-WARN] 예상하지 못한 tr_id: " + trId);
                return;
            }

            String[] fields = payload.split("\\^");
//            System.out.println("[ALARM-DEBUG] 파싱된 필드 수: " + fields.length);

            // 필드 수 체크 (호가 데이터는 보통 50개 이상)
            if (fields.length < 23) {
//                System.out.println("[ALARM-WARN] 호가 데이터 필드 수 부족: " + fields.length);
                return;
            }

            // 🔥 호가 데이터 구조에 맞게 파싱
            String stockCode = fields[0];
            String time = fields[1];

            // 🔥 호가 정보 파싱
            int askPrice1 = Integer.parseInt(fields[3]);  // 매도1호가
            int askPrice2 = Integer.parseInt(fields[4]);  // 매도2호가
            int bidPrice1 = Integer.parseInt(fields[13]); // 매수1호가
            int bidPrice2 = Integer.parseInt(fields[14]); // 매수2호가

            // 🔥 현재가 추정 로직 (매수1호가 사용)
            int estimatedCurrentPrice = bidPrice1;

            // 데이터 구성
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stock_code", stockCode);
            data.put("time", time);
            data.put("ask_price_1", askPrice1);
            data.put("ask_price_2", askPrice2);
            data.put("bid_price_1", bidPrice1);
            data.put("bid_price_2", bidPrice2);
            data.put("current_price", estimatedCurrentPrice);
            data.put("spread", askPrice1 - bidPrice1);

            // 전체 메시지 JSON 구성
            Map<String, Object> jsonMessage = new LinkedHashMap<>();
            jsonMessage.put("tr_id", trId);
            jsonMessage.put("seq", seq);
            jsonMessage.put("data", data);

            // JSON 출력
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMessage);
//            System.out.println("[ALARM-JSON] " + json);

            // 🔥 알람 처리: 추정 현재가 사용 (이벤트 발행)
//            System.out.println("[ALARM-PROCESS] 종목: " + stockCode + ", 매도1호가: " + askPrice1 + ", 매수1호가: " + bidPrice1 + ", 추정현재가: " + estimatedCurrentPrice);

            // 🔥 이벤트 발행으로 StockPriceProcessor에 전달
            if (!isApplicationShuttingDown) {
                eventPublisher.publishEvent(new StockPriceEvent(stockCode, estimatedCurrentPrice));
            }

        } catch (Exception e) {
            if (e.getMessage().contains("Singleton bean creation not allowed")) {
                isApplicationShuttingDown = true;
                return;
            }
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

    // 종료 시 호출
    @PreDestroy
    public void shutdown() {
        isApplicationShuttingDown = true;
    }


    // 🔥 알람 설정된 종목들만 구독
    private void subscribeToAlertStocks() {
        try {
            // 활성화된 알람 설정에서 종목 코드 추출
            List<String> alertStockCodes = stockAlertSettingRepository.findActiveStockCodes();

            System.out.println("[ALARM-SUBSCRIBE] 알람 설정된 종목 수: " + alertStockCodes.size());

            for (String code : alertStockCodes) {
                if (this.session != null && this.session.isOpen()) {
                    String subscribeMsg = subscribeJson(code);
                    this.session.getAsyncRemote().sendText(subscribeMsg);
                    subscribedStocks.add(code);
                    System.out.println("[ALARM-SUBSCRIBE] 구독 요청 전송: " + code);
                } else {
                    System.out.println("[ALARM-WARN] 세션이 닫혀 있어 subscribe 실패");
                }
            }
        } catch (Exception e) {
            System.err.println("[ALARM-ERROR] 알람 종목 구독 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 🔥 동적 구독 추가
    public void addSubscription(String stockCode) {
        if (subscribedStocks.contains(stockCode)) {
            System.out.println("[ALARM-INFO] 이미 구독 중인 종목: " + stockCode);
            return;
        }

        if (this.session != null && this.session.isOpen()) {
            String subscribeMsg = subscribeJson(stockCode);
            this.session.getAsyncRemote().sendText(subscribeMsg);
            subscribedStocks.add(stockCode);
            System.out.println("[ALARM-ADD] 새 종목 구독: " + stockCode);
        } else {
            pendingSubscriptions.offer(stockCode);
            System.out.println("[ALARM-PENDING] 구독 대기열에 추가: " + stockCode);
        }
    }

    // 🔥 동적 구독 해제
    public void removeSubscription(String stockCode) {
        if (!subscribedStocks.contains(stockCode)) {
            System.out.println("[ALARM-INFO] 구독하지 않은 종목: " + stockCode);
            return;
        }

        if (this.session != null && this.session.isOpen()) {
            String unsubscribeMsg = unsubscribeJson(stockCode);
            this.session.getAsyncRemote().sendText(unsubscribeMsg);
            subscribedStocks.remove(stockCode);
            System.out.println("[ALARM-REMOVE] 종목 구독 해제: " + stockCode);
        } else {
            pendingUnsubscriptions.offer(stockCode);
            System.out.println("[ALARM-PENDING] 구독 해제 대기열에 추가: " + stockCode);
        }
    }

    // 🔥 전체 구독 새로고침
    public void refreshSubscriptions() {
        try {
            System.out.println("[ALARM-REFRESH] 구독 목록 새로고침 시작");

            // 현재 활성화된 알람 종목들 조회
            List<String> currentAlertStocks = stockAlertSettingRepository.findActiveStockCodes();
            Set<String> newStockSet = new HashSet<>(currentAlertStocks);

            // 구독 해제할 종목들 (기존 구독 중이지만 알람이 없는 종목들)
            Set<String> toUnsubscribe = new HashSet<>(subscribedStocks);
            toUnsubscribe.removeAll(newStockSet);

            // 새로 구독할 종목들 (알람은 있지만 구독하지 않은 종목들)
            Set<String> toSubscribe = new HashSet<>(newStockSet);
            toSubscribe.removeAll(subscribedStocks);

            // 구독 해제
            for (String stockCode : toUnsubscribe) {
                removeSubscription(stockCode);
            }

            // 새 구독
            for (String stockCode : toSubscribe) {
                addSubscription(stockCode);
            }

            System.out.println("[ALARM-REFRESH] 구독 새로고침 완료 - 해제: " + toUnsubscribe.size() + ", 추가: " + toSubscribe.size());

        } catch (Exception e) {
            System.err.println("[ALARM-ERROR] 구독 새로고침 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
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

    // 🔥 구독 해제용 JSON
    private String unsubscribeJson(String stockCode) {
        return """
                  {
                    "header": {
                      "approval_key": "%s",
                      "custtype": "P",
                      "tr_type": "2",
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