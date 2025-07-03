package com.project.stock.investory.stockInfo.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * ✅ KIS 실시간 WebSocket 클라이언트
 *
 * - 한국투자증권(KIS)의 웹소켓 서버에 연결하여 실시간 주식 체결 데이터를 받아오는 클래스
 * - 여러 종목을 동시에 구독할 수 있고, 각 종목에 대한 listener(callback)를 등록해서 데이터 수신 시 처리 가능
 * - Spring Bean으로 등록되고, 애플리케이션 시작 시 자동 연결됨
 */
@ClientEndpoint // WebSocket 클라이언트로 동작함
@Component       // Spring이 이 클래스를 Bean으로 등록
@Slf4j
public class KisWebSocketClient {

    /* -------------------------- WebSocket 설정 -------------------------- */

    // 웹소켓 서버 URL (기본값: KIS 주소)
    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000/WebSocket}")
    private String wsUrl;

    // 승인 키 (한국투자증권 OpenAPI에서 받은 키)
    @Value("${sse.approval_key}")
    private String approvalKey;

    // 재연결 시도 관련 상수
    private static final int MAX_RECONNECT = 5;         // 최대 5번 재시도
    private static final long RECONNECT_BACKOFF_MS = 2000L; // 시도 간 간격 (점점 늘어남)

    /* --------------------------- 내부 상태 ------------------------------ */

    private final ObjectMapper om = new ObjectMapper();             // JSON 파싱용
    private final ReentrantLock connectionLock = new ReentrantLock(); // 연결/해제 시 동기화
    private final ReentrantLock sendLock = new ReentrantLock();       // 전송 시 동기화

    // 현재 구독된 종목 ID 목록 (중복 구독 방지용)
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();

    // 종목별 → 데이터 수신 시 호출할 listener 목록 (콜백함수 리스트)
    private final Map<String, CopyOnWriteArrayList<Consumer<RealTimeTradeDTO>>> listeners = new ConcurrentHashMap<>();

    private Session session;                     // WebSocket 세션 객체
    private final AtomicBoolean reconnecting = new AtomicBoolean(false); // 재연결 중인지 플래그

    /* ------------------------ 생명주기 콜백 ----------------------------- */

    // 앱 시작 후 자동 연결
    @PostConstruct
    public void init() {
        asyncConnect();
    }

    // 앱 종료 시 연결 해제
    @PreDestroy
    public void destroy() {
        connectionLock.lock();
        try {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(
                        CloseReason.CloseCodes.NORMAL_CLOSURE, "shutdown"));
            }
        } catch (Exception ignored) {
        } finally {
            connectionLock.unlock();
        }
    }

    /* ------------------------- 외부 API 제공 ---------------------------- */

    /**
     * ✅ 구독 요청 (중복 방지됨)
     *
     * - 해당 stockId에 대한 실시간 데이터를 수신하길 원하는 경우 이 메서드 사용
     * - 내부적으로 서버에 실제 subscribe 요청은 1번만 보내고, listener는 계속 추가됨
     */
    public void queueSubscribe(String stockId, Consumer<RealTimeTradeDTO> handler) {
        listeners.computeIfAbsent(stockId, k -> new CopyOnWriteArrayList<>()).add(handler);

        // 최초 구독인 경우만 서버에 subscribe 패킷 전송
        if (subscribed.add(stockId)) {
            sendLater(() -> sendSubscribe(stockId));
        } else {
            log.debug("[{}] 이미 서버에 subscribe 완료 – listener만 추가함", stockId);
        }
    }

    /**
     * ✅ 구독 해제 요청
     *
     * - 모든 listener가 제거되면 서버에도 unsubscribe 요청 보냄
     */
    public void queueUnsubscribe(String stockId) {
        listeners.remove(stockId);

        if (subscribed.remove(stockId)) {
            sendLater(() -> sendUnsubscribe(stockId));
        }
    }

    /* ---------------------- WebSocket 연결 관련 ------------------------- */

    // 비동기 WebSocket 연결 시도
    private void asyncConnect() {
        CompletableFuture.runAsync(() -> {
            connectionLock.lock();
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.setDefaultMaxSessionIdleTimeout(60_000); // 타임아웃 설정
                this.session = container.connectToServer(this, URI.create(wsUrl));
                log.info("📡 KIS WebSocket 연결 수립: {}", wsUrl);
            } catch (Exception e) {
                log.error("WebSocket 연결 실패", e);
                triggerReconnect(); // 실패 시 재연결 시도
            } finally {
                connectionLock.unlock();
            }
        });
    }

    // 재연결 로직 (점점 대기시간 늘리며 MAX_RECONNECT까지 시도)
    private void triggerReconnect() {
        if (!reconnecting.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= MAX_RECONNECT; i++) {
                try {
                    Thread.sleep(RECONNECT_BACKOFF_MS * i);
                    log.info("🔄 재연결 시도 {}/{}", i, MAX_RECONNECT);
                    asyncConnect();
                    return; // 연결 성공하면 루프 종료
                } catch (InterruptedException ignored) {
                }
            }
            log.error("❌ WebSocket 재연결 실패 – 종료됨");
        }).whenComplete((v, t) -> reconnecting.set(false));
    }

    /* ---------------------- 실제 전송 함수들 ---------------------------- */

    // 서버에 subscribe 패킷 전송
    private void sendSubscribe(String stockId) {
        sendLock.lock();
        try {
            if (!isSessionOpen()) return;
            session.getBasicRemote().sendText(buildPayload("1", stockId)); // 1 = subscribe
            log.info("▶️ 서버에 subscribe 전송: {}", stockId);
        } catch (Exception e) {
            log.error("subscribe 전송 오류 – {}", stockId, e);
            subscribed.remove(stockId); // 실패 시 다시 구독 가능하도록
        } finally {
            sendLock.unlock();
        }
    }

    // 서버에 unsubscribe 패킷 전송
    private void sendUnsubscribe(String stockId) {
        sendLock.lock();
        try {
            if (!isSessionOpen()) return;
            session.getBasicRemote().sendText(buildPayload("2", stockId)); // 2 = unsubscribe
            log.info("⏹️ 서버에 unsubscribe 전송: {}", stockId);
        } catch (Exception e) {
            log.error("unsubscribe 전송 오류 – {}", stockId, e);
        } finally {
            sendLock.unlock();
        }
    }

    // 현재 세션 열려있는지 여부 확인
    private boolean isSessionOpen() {
        return session != null && session.isOpen();
    }

    // 연결 상태 아닐 경우 연결 재시도 후 전송
    private void sendLater(Runnable task) {
        CompletableFuture.runAsync(() -> {
            if (!isSessionOpen()) {
                asyncConnect();
                try {
                    Thread.sleep(300); // 약간의 대기 시간
                } catch (InterruptedException ignored) {}
            }
            task.run();
        });
    }

    // ✅ KIS 실시간 체결 요청용 payload 생성기 (JSON 형식)
    private String buildPayload(String trType, String stockId) {
        return """
                {
                  "header": {
                    "approval_key": "%s",
                    "custtype":    "P",
                    "tr_type":     "%s",
                    "content-type":"utf-8",
                    "tr_id":       "H0NXCNT0",
                    "tr_key":      "%s"
                  },
                  "body": {
                    "input": {
                      "tr_id":  "H0NXCNT0",
                      "tr_key": "%s"
                    }
                  }
                }
                """.replace("\n", "") // 줄바꿈 제거
                .formatted(approvalKey, trType, stockId, stockId);
    }

    /* --------------------- WebSocket 이벤트 콜백 ------------------------ */

    @OnOpen
    public void onOpen(Session s) {
        log.info("✅ WebSocket OPEN 성공");
        this.session = s;

        // 재연결된 경우, 기존 구독 종목 다시 전송
        subscribed.forEach(this::sendSubscribe);
    }

    @OnMessage
    public void onMessage(String msg) {
        if (msg.contains("\"tr_id\":\"PINGPONG\"")) return; // 하트비트는 무시

        if (msg.startsWith("0|H0NXCNT0|")) { // 실시간 체결 데이터 (nxt)
            handleRealtime(msg);
        } else if (msg.startsWith("{")) {    // JSON 형식 응답
            handleJson(msg);
        } else {
            log.debug("알 수 없는 메시지: {}", msg);
        }
    }

    @OnClose
    public void onClose(Session s, CloseReason reason) {
        log.warn("⚠️ WebSocket CLOSED: {} ({})", reason.getReasonPhrase(), reason.getCloseCode());
        triggerReconnect(); // 연결 끊겼을 때 재시도
    }

    @OnError
    public void onError(Session s, Throwable t) {
        log.error("❌ WebSocket ERROR 발생", t);
    }

    /* ---------------------- 메시지 처리 로직 ---------------------------- */

    // 실시간 체결 데이터 파싱 및 listener에게 전달
    private void handleRealtime(String raw) {
        try {
            String[] parts = raw.split("\\|", 4);
            if (parts.length < 4) return;

            String[] f = parts[3].split("\\^"); // 실데이터 분해
            String code = f[0];                 // 종목코드

            List<Consumer<RealTimeTradeDTO>> list = listeners.get(code);
            if (list == null || list.isEmpty()) return;

            RealTimeTradeDTO dto = RealTimeTradeDTO.from(f); // 변환
            list.forEach(cb -> {
                try {
                    cb.accept(dto); // 콜백 실행
                } catch (Exception e) {
                    log.warn("listener 예외 발생", e);
                }
            });

        } catch (Exception e) {
            log.warn("실시간 패킷 파싱 실패: {}", raw, e);
        }
    }

    // 서버 응답 (JSON) 파싱
    private void handleJson(String json) {
        try {
            JsonNode body = om.readTree(json).path("body");
            String rt = body.path("rt_cd").asText(); // 응답 코드

            if ("0".equals(rt)) {
                log.info("✅ 구독 성공 – {}", body.path("tr_key").asText(""));
            } else {
                String code = body.path("msg_cd").asText();
                String msg = body.path("msg1").asText();
                log.warn("❗ 구독 실패 ({}) {}", code, msg);
            }
        } catch (Exception e) {
            log.warn("JSON 메시지 파싱 실패: {}", json, e);
        }
    }
}
