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

    @Value("${sse.approval_key}")
    private String approvalKey;

    private final ObjectMapper om = new ObjectMapper();
    private Session session;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // 종목별 데이터 핸들러 저장
    private final Map<String, Consumer<RealTimeTradeDTO>> handlers = new ConcurrentHashMap<>();

    // 재연결 관련
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_MS = 2000;

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
                log.error("WebSocket 연결 실패: {}", stockId, e);
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

        // 구독 해제는 안전하게 처리
        try {
            sendUnsubscribeMessage(stockId);
        } catch (Exception e) {
            log.warn("구독 해제 중 오류 발생: {}", stockId, e);
        }

        // 모든 핸들러가 제거되면 WebSocket 연결 종료
        if (handlers.isEmpty()) {
            disconnect();
        }
    }

    /* ========= 내부 메서드 ========= */

    private void connect() throws Exception {
        if (isConnected.get()) return;

        log.info("KIS WebSocket 연결 시도: {}", wsUrl);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        // 타임아웃 설정
        container.setDefaultMaxSessionIdleTimeout(60000); // 60초
        container.setAsyncSendTimeout(5000); // 5초

        container.connectToServer(this, URI.create(wsUrl));
    }

    private void sendSubscribeMessage(String stockId) {
        if (!isSessionValid()) {
            log.warn("WebSocket 세션이 유효하지 않음: {}", stockId);
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
        if (!isSessionValid()) {
            log.debug("WebSocket 세션이 유효하지 않아 구독 해제 생략: {}", stockId);
            return;
        }

        // 구독 해제 메시지 수정 - body 추가
        String payload = """
        {
          "header": {
            "approval_key": "%s",
            "custtype": "P",
            "tr_type": "2",
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
            log.info("종목 {} 실시간 데이터 구독 해제", stockId);
        } catch (Exception e) {
            log.error("구독 해제 메시지 전송 실패: {}", stockId, e);
        }
    }

    private boolean isSessionValid() {
        return session != null && session.isOpen() && isConnected.get();
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
        isReconnecting.set(false);
        log.info("KIS WebSocket 연결 성공!");
    }

    @OnMessage
    public void onMessage(String message) {
        try {
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
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생", e);
        }
    }

    private void handleRealTimeData(String rawData) {
        try {
            String[] parts = rawData.split("\\|", 4);
            if (parts.length < 4) {
                log.debug("실시간 데이터 형식 오류: 부분 수 {}", parts.length);
                return;
            }

            String[] fields = parts[3].split("\\^");
            if (fields.length < 40) {
                log.warn("실시간 데이터 필드 수 부족: {}", fields.length);
                return;
            }

            String stockId = fields[0];

            // 핸들러가 있는지 확인
            Consumer<RealTimeTradeDTO> handler = handlers.get(stockId);
            if (handler == null) {
                log.debug("핸들러가 없는 종목 데이터 수신: {}", stockId);
                return;
            }

            RealTimeTradeDTO dto = new RealTimeTradeDTO(
                    stockId,                    // 종목코드
                    fields[2],                  // 현재가
                    fields[12],                 // 체결량
                    fields[5] + "%",           // 등락율
                    fields[13],                 // 누적거래량
                    fields[1]                   // 체결시간
            );

            // 해당 종목의 핸들러에게 데이터 전달
            handler.accept(dto);

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
                    log.debug("이미 연결된 상태입니다: {}", msg);
                } else if ("OPSP9992".equals(msgCode)) {
                    log.warn("JSON 파싱 오류 (구독 해제 시 정상): {}", msg);
                } else {
                    log.error("KIS 오류 - 코드: {}, 메시지: {}", msgCode, msg);
                }
                return;
            }

            log.debug("구독 성공 응답 수신");

        } catch (Exception e) {
            log.error("JSON 응답 처리 오류", e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        this.session = null;
        boolean wasConnected = isConnected.getAndSet(false);

        log.info("KIS WebSocket 연결 종료: {} (코드: {})",
                closeReason.getReasonPhrase(), closeReason.getCloseCode());

        // 비정상 종료인 경우 재연결 시도
        if (wasConnected && !handlers.isEmpty() &&
                closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {

            attemptReconnect();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("KIS WebSocket 오류", throwable);
        isConnected.set(false);

        // 핸들러가 있으면 재연결 시도
        if (!handlers.isEmpty()) {
            attemptReconnect();
        }
    }

    private void attemptReconnect() {
        if (isReconnecting.getAndSet(true)) {
            return; // 이미 재연결 중
        }

        new Thread(() -> {
            for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS; attempt++) {
                try {
                    log.info("WebSocket 재연결 시도 {}/{}", attempt, MAX_RECONNECT_ATTEMPTS);
                    Thread.sleep(RECONNECT_DELAY_MS * attempt); // 지수 백오프

                    connect();

                    // 기존 구독 복구
                    for (String stockId : handlers.keySet()) {
                        sendSubscribeMessage(stockId);
                    }

                    log.info("WebSocket 재연결 성공");
                    return;

                } catch (Exception e) {
                    log.warn("재연결 시도 {}/{} 실패", attempt, MAX_RECONNECT_ATTEMPTS, e);
                }
            }

            log.error("WebSocket 재연결 최대 시도 횟수 초과");
            isReconnecting.set(false);
        }).start();
    }
}