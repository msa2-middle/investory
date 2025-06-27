package com.project.stock.investory.stockInfo.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockAlertSetting.processor.StockPriceProcessor;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import com.project.stock.investory.stockInfo.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@ClientEndpoint
@Component
@RequiredArgsConstructor
@Slf4j
public class KisWebSocketClient {

    private final StockPriceProcessor stockPriceProcessor;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000/WebSocket}")
    private String wsUrl;

    // 기존 메서드명 유지를 위해 두 가지 property 모두 지원
    @Value("${koreainvest.approval-key:${kis.approval-key}}")
    private String approvalKey;

    private Session session;
    private ScheduledExecutorService heartbeatExecutor;
    private final Set<String> subs = ConcurrentHashMap.newKeySet();

    // 실시간 거래 데이터 핸들러 (KisWebSocketClient 기능)
    private Consumer<RealTimeTradeDTO> handler;

    @PostConstruct
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(wsUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        disconnect();
    }

    /* ========= KisWebSocketClient 호환 메서드들 ========= */

    /**
     * 실시간 거래 데이터 구독 (KisWebSocketClient 호환)
     */
    public synchronized void subscribe(String stockId, Consumer<RealTimeTradeDTO> cb) throws Exception {
        this.handler = cb;

        // 이미 WebSocket 세션이 열려 있으면 새 연결 대신 구독 메시지만 전송
        if (session != null && session.isOpen()) {
            sendSubMsg(stockId);
            return;
        }

        // 최초 연결
        connect();
        sendSubMsg(stockId);
    }

    /**
     * WebSocket 연결 해제 (KisWebSocketClient 호환)
     */
    public synchronized void disconnect() {
        try {
            if (session != null && session.isOpen()) {
                for (String id : subs) {
                    String unsub = """
                    {
                      "header": {
                        "approval_key":"%s",
                        "custtype":"P",
                        "tr_type":"2",
                        "content-type":"utf-8",
                        "tr_id":"H0STCNT0",
                        "tr_key":"%s"
                      }
                    }
                    """.formatted(approvalKey, id);
                    session.getAsyncRemote().sendText(unsub);
                }
                session.close(new CloseReason(
                        CloseReason.CloseCodes.NORMAL_CLOSURE, "manual close"));
                log.info("KIS WS 정상 종료");
            }
        } catch (Exception e) {
            log.warn("WS 정상 종료 실패", e);
        } finally {
            subs.clear();
            session = null;
            stopHeartbeat();
        }
    }

    private void sendSubMsg(String stockId) {
        if (subs.contains(stockId)) return;

        String payload = """
        {
          "header": {
            "approval_key": "%s",
            "custtype": "P",
            "tr_type": "1",
            "content-type":"utf-8",
            "tr_id": "H0STCNT0",
            "tr_key": "%s"
          },
          "body": {
            "input": {
              "tr_id": "H0STCNT0",
              "tr_key": "%s"
            }
          }
        }""".formatted(approvalKey, stockId, stockId);

        session.getAsyncRemote().sendText(payload);
        subs.add(stockId);
    }

    /* ========= WebSocket 이벤트 핸들러 ========= */

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[OPEN] 연결됨");

        // DB에서 종목코드 가져오기 (KisWebSocketClientAlarm 기능)
        List<String> stockCodes = stockRepository.findAllStockCodes();

        // 승인 요청
        session.getAsyncRemote().sendText(approvalJson());

        // 1초 후 종목별로 subscribe 전송
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            for (String code : stockCodes) {
                if (this.session != null && this.session.isOpen()) {
                    String subscribeMsg = subscribeJson(code);
                    this.session.getAsyncRemote().sendText(subscribeMsg);
                    System.out.println("[SUBSCRIBE] 구독 요청 전송: " + subscribeMsg);
                    subs.add(code); // subs에도 추가
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
            /* 0) 하트비트 무시 (KisWebSocketClient 기능) */
            if (message.contains("\"tr_id\":\"PINGPONG\"")) return;

            /* 1) JSON 메시지일 경우 처리 */
            if (message.trim().startsWith("{")) {
                handleJsonMessage(message);
                return;
            }

            /* 2) 파이프 구분 실시간 데이터 처리 */
            String[] parts = message.split("\\|");
            if (parts.length < 4) return;

            String trId = parts[1];
            String seq = parts[2];
            String payload = parts[3];

            if (!"H0STCNT0".equals(trId)) return;

            String[] fields = payload.split("\\^");

            // KisWebSocketClient 스타일 처리 (간단한 DTO)
            if (fields.length >= 40) {
                RealTimeTradeDTO dto = new RealTimeTradeDTO(
                        fields[0],          // stockId  (STCK_SHRN_ISCD)
                        fields[2],          // tradePrice (STCK_PRPR)
                        fields[12],         // tradeVolume (CNTG_VOL)
                        fields[5] + "%",    // changeRate  (PRDY_CTRT)
                        fields[13],         // accumulateVolume (ACML_VOL)
                        fields[1]           // tradeTime (STCK_CNTG_HOUR)
                );

                log.info("[KIS DTO] {}", dto);
                if (handler != null) handler.accept(dto);
            }

            // KisWebSocketClientAlarm 스타일 처리 (상세한 맵 구조)
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

            Map<String, Object> jsonMessage = new LinkedHashMap<>();
            jsonMessage.put("tr_id", trId);
            jsonMessage.put("seq", seq);
            jsonMessage.put("data", data);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMessage);
            System.out.println(json);

            // JSON 문자열 파싱하여 알림 처리
            JsonNode root = objectMapper.readTree(json);
            String stockCode = root.at("/data/stock_code").asText();
            int currentPrice = root.at("/data/current_price").asInt();

            System.out.println(stockCode + "   " + currentPrice);

            // 알림 처리 (KisWebSocketClientAlarm 기능)
            stockPriceProcessor.process(stockCode, currentPrice);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleJsonMessage(String message) {
        log.debug("[KIS RAW] {}", message);
        try {
            JsonNode b = objectMapper.readTree(message).path("body");
            if (!"0".equals(b.path("rt_cd").asText())) {
                log.error("KIS 오류 {} - {}", b.path("msg_cd").asText(),
                        b.path("msg1").asText());
                return;
            }

            // JSON 형태의 실시간 데이터 처리
            if (b.has("STCK_SHRN_ISCD")) {
                RealTimeTradeDTO dto = new RealTimeTradeDTO(
                        b.path("STCK_SHRN_ISCD").asText(),
                        b.path("STCK_PRPR").asText(),
                        b.path("CNTG_VOL").asText(),
                        b.path("PRDY_CTRT").asText() + "%",
                        b.path("ACML_VOL").asText(),
                        b.path("STCK_CNTG_HOUR").asText()
                );
                log.info("[KIS DTO] {}", dto);
                if (handler != null) handler.accept(dto);
            }
        } catch (Exception e) {
            log.warn("JSON 파싱 오류", e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[CLOSE] 세션 종료: " + reason);
        System.out.println("[CLOSE CODE] " + reason.getCloseCode().getCode());
        stopHeartbeat();

        this.session = null;
        subs.clear();

        // 비정상 종료인 경우 재연결 시도 (선택적 - 기존 주석 처리된 코드 참고)
        // Executors.newSingleThreadScheduledExecutor().schedule(() -> {
        //     System.out.println("[RECONNECT] 5초 후 재연결 시도");
        //     connect();
        // }, 5, TimeUnit.SECONDS);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[ERROR] 에러 발생: " + throwable.getMessage());
        throwable.printStackTrace();
        stopHeartbeat();
    }

    /* ========= 하트비트 관리 ========= */

    private void startHeartbeat() {
        // 하트비트를 비활성화 - 서버에서 자체적으로 PINGPONG 처리
        System.out.println("[INFO] 하트비트 비활성화 - 서버 자체 PINGPONG 사용");
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
        }
    }

    /* ========= 메시지 생성 메서드 (기존 메서드명 유지) ========= */

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

//package com.project.stock.investory.stockInfo.websocket;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
//import jakarta.websocket.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.function.Consumer;
//
//@ClientEndpoint
//@Component
//@Slf4j
//public class KisWebSocketClient {
//
//    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000/WebSocket}")
//    private String wsUrl;
//
//    @Value("${kis.approval-key}")
//    private String approvalKey;
//
//    private final ObjectMapper om = new ObjectMapper();
//    private Consumer<RealTimeTradeDTO> handler;            // 서비스층 콜백
//    private final Set<String> subs = ConcurrentHashMap.newKeySet();
//    private Session session;
//
//    /* ========= 외부 API ========= */
//
//    public synchronized void subscribe(String stockId,
//                                       Consumer<RealTimeTradeDTO> cb) throws Exception {
//        this.handler = cb;          // 서비스에서 넘겨준 콜백 저장
//
//        // 이미 WebSocket 세션이 열려 있으면 새 연결 대신 구독 메시지만 전송
//        if (session != null && session.isOpen()) {
//            sendSubMsg(stockId);
//            return;
//        }
//
//        // 최초 연결
//        connect();
//        sendSubMsg(stockId);
//    }
//
//    /* ========= 내부 ========= */
//
//    private void connect() throws Exception {
//        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
//        c.connectToServer(this, URI.create(wsUrl));
//    }
//
//
//    public synchronized void disconnect() {
//        try {
//            if (session != null && session.isOpen()) {
//                for (String id : subs) {              // 🔹 subs 로 변경
//                    String unsub = """
//                    {
//                      "header": {
//                        "approval_key":"%s",
//                        "custtype":"P",
//                        "tr_type":"2",           // 해제
//                        "content-type":"utf-8",
//                        "tr_id":"H0STCNT0",
//                        "tr_key":"%s"
//                      }
//                    }
//                    """.formatted(approvalKey, id);
//                    session.getAsyncRemote().sendText(unsub);
//                }
//                // Close Frame
//                session.close(new CloseReason(
//                        CloseReason.CloseCodes.NORMAL_CLOSURE, "manual close"));
//                log.info("KIS WS 정상 종료");
//            }
//        } catch (Exception e) {
//            log.warn("WS 정상 종료 실패", e);
//        } finally {
//            subs.clear();
//            session = null;
//        }
//    }
//
//    private void sendSubMsg(String stockId) {
//        if (subs.contains(stockId)) return;
//
//        String payload = """
//        {
//          "header": {
//            "approval_key": "%s",
//            "custtype": "P",
//            "tr_type": "1",
//            "content-type":"utf-8",
//            "tr_id": "H0STCNT0",
//            "tr_key": "%s"
//          },
//          "body": {
//            "input": {
//              "tr_id": "H0STCNT0",
//              "tr_key": "%s"
//            }
//          }
//        }""".formatted(approvalKey, stockId, stockId);
//
//        session.getAsyncRemote().sendText(payload);
//        subs.add(stockId);
//    }
//
//    /* ========= WS 콜백 ========= */
//
//    @OnOpen
//    public void onOpen(Session s) { this.session = s; log.info("KIS WS 연결"); }
//
//    @OnMessage
//    public void onMsg(String raw) {
//
//        /* 0) 하트비트 무시 */
//        if (raw.contains("\"tr_id\":\"PINGPONG\"")) return;
//
//        /* 1) 문자열(H0STCNT0) 패킷 처리 ---------------------------------- */
//        if (raw.startsWith("0|H0STCNT0|")) {
//            log.debug("[KIS RAW] {}", raw);
//
//            // 파이프 3개(0|H0STCNT0|001|) 이후 부분만 캐럿(^)으로 분리
//            String[] pipe = raw.split("\\|", 4);
//            if (pipe.length < 4) return;
//            String[] f = pipe[3].split("\\^");
//
//            // 안전 체크 (최소 40여 개 필드)
//            if (f.length < 40) {
//                log.warn("필드 수 부족: {}", f.length);
//                return;
//            }
//
//            /* === 원하는 값 추출 === */
//            RealTimeTradeDTO dto = new RealTimeTradeDTO(
//                    f[0],          // stockId  (STCK_SHRN_ISCD)
//                    f[2],          // tradePrice (STCK_PRPR)
//                    f[12],         // tradeVolume (CNTG_VOL)  ← 6 ▶ 12 로
//                    f[5] + "%",    // changeRate  (PRDY_CTRT) ← 4 ▶ 5 로
//                    f[13],         // accumulateVolume (ACML_VOL)
//                    f[1]           // tradeTime (STCK_CNTG_HOUR)
//            );
//
//            log.info("[KIS DTO] {}", dto);
//            if (handler != null) handler.accept(dto);
//            return;
//        }
//
//        /* 2) JSON 응답 패킷 처리 ---------------------------------------- */
//        if (raw.startsWith("{")) {
//            log.debug("[KIS RAW] {}", raw);
//            try {
//                JsonNode b = om.readTree(raw).path("body");
//                if (!"0".equals(b.path("rt_cd").asText())) {  // 오류 응답
//                    log.error("KIS 오류 {} - {}", b.path("msg_cd").asText(),
//                            b.path("msg1").asText());
//                    return;
//                }
//                RealTimeTradeDTO dto = new RealTimeTradeDTO(
//                        b.path("STCK_SHRN_ISCD").asText(),
//                        b.path("STCK_PRPR").asText(),
//                        b.path("CNTG_VOL").asText(),
//                        b.path("PRDY_CTRT").asText() + "%",
//                        b.path("ACML_VOL").asText(),
//                        b.path("STCK_CNTG_HOUR").asText()
//                );
//                log.info("[KIS DTO] {}", dto);
//                if (handler != null) handler.accept(dto);
//            } catch (Exception e) {
//                log.warn("JSON 파싱 오류", e);
//            }
//        }
//    }
//
//    @OnError
//    public void onErr(Session s, Throwable t) { log.error("WS 오류", t); }
//}
