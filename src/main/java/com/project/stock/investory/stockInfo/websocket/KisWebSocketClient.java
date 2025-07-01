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
 * KIS 실시간(웹소켓) 커넥터 – subscribe once & fan-out
 */
@ClientEndpoint
@Component
@Slf4j
public class KisWebSocketClient {

    /* ------------------------------------------------------------------ */
    /* ✨ 설정                                                             */
    /* ------------------------------------------------------------------ */
    @Value("${kis.ws.url:ws://ops.koreainvestment.com:21000/WebSocket}")
    private String wsUrl;

    @Value("${sse.approval_key}")
    private String approvalKey;

    private static final int MAX_RECONNECT = 5;
    private static final long RECONNECT_BACKOFF_MS = 2_000L;

    /* ------------------------------------------------------------------ */
    /* 상태                                                               */
    /* ------------------------------------------------------------------ */
    private final ObjectMapper om = new ObjectMapper();
    private final ReentrantLock connectionLock = new ReentrantLock();   // connect/close
    private final ReentrantLock sendLock       = new ReentrantLock();   // sendText

    /** 서버에 실제 subscribe 되어 있는 종목 */
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();
    /** 종목별 → Listener 목록 */
    private final Map<String, CopyOnWriteArrayList<Consumer<RealTimeTradeDTO>>> listeners =
            new ConcurrentHashMap<>();

    private Session session;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    /* ------------------------------------------------------------------ */
    /* Life-cycle                                                         */
    /* ------------------------------------------------------------------ */

    @PostConstruct
    public void init() {
        asyncConnect();
    }

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

    /* ------------------------------------------------------------------ */
    /* 외부 API – Service 쪽에서 호출                                      */
    /* ------------------------------------------------------------------ */

    /** “한 번만” subscribe – 같은 종목이 여러 번 들어와도 서버엔 1패킷만 전송 */
    public void queueSubscribe(String stockId, Consumer<RealTimeTradeDTO> handler) {
        listeners.computeIfAbsent(stockId, k -> new CopyOnWriteArrayList<>()).add(handler);

        if (subscribed.add(stockId)) {
            // 최초 subscribe 패킷
            sendLater(() -> sendSubscribe(stockId));
        } else {
            log.debug("[{}] 이미 서버에 subscribe 완료 – listener 추가만", stockId);
        }
    }

    /** 모든 listener 가 빠지면 unsubscribe */
    public void queueUnsubscribe(String stockId) {
        listeners.remove(stockId);

        if (subscribed.remove(stockId)) {
            sendLater(() -> sendUnsubscribe(stockId));
        }
    }

    /* ------------------------------------------------------------------ */
    /* WebSocket 연결 helpers                                              */
    /* ------------------------------------------------------------------ */

    private void asyncConnect() {
        CompletableFuture.runAsync(() -> {
            connectionLock.lock();
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.setDefaultMaxSessionIdleTimeout(60_000);
                this.session = container.connectToServer(this, URI.create(wsUrl));
                log.info("📡 KIS WebSocket 연결 수립 {}", wsUrl);
            } catch (Exception e) {
                log.error("초기 WebSocket 연결 실패", e);
                triggerReconnect();
            } finally {
                connectionLock.unlock();
            }
        });
    }

    private void triggerReconnect() {
        if (!reconnecting.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= MAX_RECONNECT; i++) {
                try {
                    Thread.sleep(RECONNECT_BACKOFF_MS * i);
                    log.info("🔄 재연결 시도 {}/{}", i, MAX_RECONNECT);
                    asyncConnect();
                    return;      // 성공하면 루프 종료
                } catch (InterruptedException ignored) {
                }
            }
            log.error("❌ WebSocket 재연결 실패 – 더 이상 시도하지 않음");
        }).whenComplete((v, t) -> reconnecting.set(false));
    }

    /* ------------------------------------------------------------------ */
    /* 실제 subscribe / unsubscribe 패킷 전송                              */
    /* ------------------------------------------------------------------ */

    private void sendSubscribe(String stockId) {
        sendLock.lock();
        try {
            if (!isSessionOpen()) return;
            session.getBasicRemote().sendText(buildPayload("1", stockId));
            log.info("▶️ subscribe : {}", stockId);
        } catch (Exception e) {
            log.error("subscribe 전송 오류 – {}", stockId, e);
            subscribed.remove(stockId);
        } finally {
            sendLock.unlock();
        }
    }

    private void sendUnsubscribe(String stockId) {
        sendLock.lock();
        try {
            if (!isSessionOpen()) return;
            session.getBasicRemote().sendText(buildPayload("2", stockId));
            log.info("⏹️ unsubscribe : {}", stockId);
        } catch (Exception e) {
            log.error("unsubscribe 전송 오류 – {}", stockId, e);
        } finally {
            sendLock.unlock();
        }
    }

    private boolean isSessionOpen() {
        return session != null && session.isOpen();
    }

    private void sendLater(Runnable task) {
        CompletableFuture.runAsync(() -> {
            if (!isSessionOpen()) {
                asyncConnect();
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
            task.run();
        });
    }

    //krx
//    private String buildPayload(String trType, String stockId) {
//        return """
//               {"header":{"approval_key":"%s","custtype":"P","tr_type":"%s",
//               "content-type":"utf-8","tr_id":"H0STCNT0","tr_key":"%s"},
//               "body":{"input":{"tr_id":"H0STCNT0","tr_key":"%s"}}}
//               """.replace("\n", "")
//                .formatted(approvalKey, trType, stockId, stockId);
//    }

    private String buildPayload(String trType, String stockId) {
        return """
               {"header":{"approval_key":"%s","custtype":"P","tr_type":"%s",
               "content-type":"utf-8","tr_id":"H0NXCNT0","tr_key":"%s"},
               "body":{"input":{"tr_id":"H0NXCNT0","tr_key":"%s"}}}
               """.replace("\n", "")
                .formatted(approvalKey, trType, stockId, stockId);
    }

    //nxt

    /* ------------------------------------------------------------------ */
    /* WebSocket 콜백                                                      */
    /* ------------------------------------------------------------------ */

    @OnOpen
    public void onOpen(Session s) {
        log.info("✅  WebSocket OPEN");
        this.session = s;
        // 끊겼다가 복구됐을 때 기존 구독 복원
        subscribed.forEach(this::sendSubscribe);
    }

    @OnMessage
    public void onMessage(String msg) {
        if (msg.contains("\"tr_id\":\"PINGPONG\"")) return; // 하트비트

        if (msg.startsWith("0|H0NXCNT0|")) { // nxt
//        if (msg.startsWith("0|H0STCNT0|")) { //krx
            handleRealtime(msg);
        } else if (msg.startsWith("{")) {
            handleJson(msg);
        } else {
            log.debug("알 수 없는 메시지: {}", msg);
        }
    }

    @OnClose
    public void onClose(Session s, CloseReason reason) {
        log.warn("⚠️  WebSocket CLOSED : {} ({})",
                reason.getReasonPhrase(), reason.getCloseCode());
        triggerReconnect();
    }

    @OnError
    public void onError(Session s, Throwable t) {
        log.error("WebSocket ERROR", t);
    }

    /* ------------------------------------------------------------------ */
    /* 메시지 파서                                                         */
    /* ------------------------------------------------------------------ */

    private void handleRealtime(String raw) {
        try {
            String[] parts = raw.split("\\|", 4);
            if (parts.length < 4) return;

            String[] f = parts[3].split("\\^");
            String code = f[0];

            List<Consumer<RealTimeTradeDTO>> list = listeners.get(code);
            if (list == null || list.isEmpty()) return;

            RealTimeTradeDTO dto = RealTimeTradeDTO.from(f);
            list.forEach(cb -> {
                try { cb.accept(dto); }
                catch (Exception e) { log.warn("listener 예외", e); }
            });

        } catch (Exception e) {
            log.warn("realtime 패킷 파싱 실패: {}", raw, e);
        }
    }

    private void handleJson(String json) {
        try {
            JsonNode body = om.readTree(json).path("body");
            String rt = body.path("rt_cd").asText();
            if ("0".equals(rt)) {
                log.info("구독 성공 – {}", body.path("tr_key").asText(""));
            } else {
                String code = body.path("msg_cd").asText();
                String msg  = body.path("msg1").asText();
                log.warn("❗ KIS 오류 ({}) {}", code, msg);
            }
        } catch (Exception e) {
            log.warn("JSON 메시지 파싱 실패: {}", json, e);
        }
    }
}
