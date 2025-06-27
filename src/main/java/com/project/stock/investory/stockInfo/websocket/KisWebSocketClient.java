package com.project.stock.investory.stockInfo.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@ClientEndpoint
@Component
@Slf4j
public class KisWebSocketClient {

    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000/WebSocket}")
    private String wsUrl;

    @Value("${kis.approval-key}")
    private String approvalKey;

    private final ObjectMapper om = new ObjectMapper();
    private Session session;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // 종목별 데이터 핸들러 저장
    private final Map<String, Consumer<RealTimeTradeDTO>> handlers = new ConcurrentHashMap<>();

    /* ========= 외부 API ========= */

    /**
     * 특정 종목의 실시간 데이터를 받기 시작
     */
    public void startListening(String stockId, Consumer<RealTimeTradeDTO> dataHandler) {
        handlers.put(stockId, dataHandler);

        // WebSocket 연결이 안되어 있으면 연결
        if (!isConnected.get()) {
            try {
                connect();
            } catch (Exception e) {
                log.error("WebSocket 연결 실패", e);
                return;
            }
        }

        // 구독 메시지 전송
        sendSubscribeMessage(stockId);
    }

    /**
     * 특정 종목의 실시간 데이터 받기 중단
     */
    public void stopListening(String stockId) {
        handlers.remove(stockId);
        sendUnsubscribeMessage(stockId);
    }

    /* ========= 내부 메서드 ========= */

    private void connect() throws Exception {
        if (isConnected.get()) return;

        log.info("KIS WebSocket 연결 시도: {}", wsUrl);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, URI.create(wsUrl));
    }

    private void sendSubscribeMessage(String stockId) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket 세션이 열려있지 않음");
            return;
        }

        String payload = """
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
        }""".formatted(approvalKey, stockId, stockId);

        try {
            session.getAsyncRemote().sendText(payload);
            log.info("종목 {} 실시간 데이터 구독 시작", stockId);
        } catch (Exception e) {
            log.error("구독 메시지 전송 실패: {}", stockId, e);
        }
    }

    private void sendUnsubscribeMessage(String stockId) {
        if (session == null || !session.isOpen()) return;

        String payload = """
        {
          "header": {
            "approval_key": "%s",
            "custtype": "P",
            "tr_type": "2",
            "content-type": "utf-8",
            "tr_id": "H0STCNT0",
            "tr_key": "%s"
          }
        }""".formatted(approvalKey, stockId);

        try {
            session.getAsyncRemote().sendText(payload);
            log.info("종목 {} 실시간 데이터 구독 해제", stockId);
        } catch (Exception e) {
            log.error("구독 해제 메시지 전송 실패: {}", stockId, e);
        }
    }

    public void disconnect() {
        isConnected.set(false);
        handlers.clear();

        if (session != null && session.isOpen()) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "shutdown"));
                log.info("KIS WebSocket 연결 종료");
            } catch (Exception e) {
                log.warn("WebSocket 종료 실패", e);
            }
        }
    }

    /* ========= WebSocket 콜백 ========= */

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        isConnected.set(true);
        log.info("KIS WebSocket 연결 성공!");
    }

    @OnMessage
    public void onMessage(String message) {
        // 하트비트 무시
        if (message.contains("\"tr_id\":\"PINGPONG\"")) {
            return;
        }

        // 실시간 데이터 처리 (문자열 형태)
        if (message.startsWith("0|H0STCNT0|")) {
            handleRealTimeData(message);
            return;
        }

        // JSON 응답 처리
        if (message.startsWith("{")) {
            handleJsonResponse(message);
        }
    }

    private void handleRealTimeData(String rawData) {
        try {
            String[] parts = rawData.split("\\|", 4);
            if (parts.length < 4) return;

            String[] fields = parts[3].split("\\^");
            if (fields.length < 40) {
                log.warn("실시간 데이터 필드 수 부족: {}", fields.length);
                return;
            }

            String stockId = fields[0];
            RealTimeTradeDTO dto = new RealTimeTradeDTO(
                    stockId,                    // 종목코드
                    fields[2],                  // 현재가
                    fields[12],                 // 체결량
                    fields[5] + "%",           // 등락율
                    fields[13],                 // 누적거래량
                    fields[1]                   // 체결시간
            );

            // 해당 종목의 핸들러에게 데이터 전달
            Consumer<RealTimeTradeDTO> handler = handlers.get(stockId);
            if (handler != null) {
                handler.accept(dto);
            }

        } catch (Exception e) {
            log.error("실시간 데이터 처리 오류", e);
        }
    }

    private void handleJsonResponse(String json) {
        try {
            JsonNode root = om.readTree(json);
            JsonNode body = root.path("body");

            String rtCode = body.path("rt_cd").asText();
            if (!"0".equals(rtCode)) {
                String msgCode = body.path("msg_cd").asText();
                String msg = body.path("msg1").asText();

                if ("OPSP8996".equals(msgCode)) {
                    log.info("이미 연결된 상태입니다: {}", msg);
                } else {
                    log.error("KIS 오류 - 코드: {}, 메시지: {}", msgCode, msg);
                }
                return;
            }

            log.info("구독 성공 응답 수신");

        } catch (Exception e) {
            log.error("JSON 응답 처리 오류", e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        this.session = null;
        isConnected.set(false);
        log.info("KIS WebSocket 연결 종료: {}", closeReason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("KIS WebSocket 오류", throwable);
        isConnected.set(false);
    }
}

