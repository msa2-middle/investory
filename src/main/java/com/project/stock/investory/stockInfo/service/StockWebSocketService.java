package com.project.stock.investory.stockInfo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import com.project.stock.investory.stockInfo.util.StockMarketUtils;
import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class StockWebSocketService {

    private final KisWebSocketClient kisClient;
    private final ObjectMapper om = new ObjectMapper();

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public StockWebSocketService(KisWebSocketClient kisClient) {
        this.kisClient = kisClient;
    }

    public SseEmitter getStockPriceStream(String stockId) {


        // ① 장 외 시간이면 종료
        if (!StockMarketUtils.isTradingHours()) {
            return closedEmitter("marketClosed", "장 외 시간입니다.");
        }



        // ② 새 emitter 생성 (30분 timeout)
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        emitters.computeIfAbsent(stockId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("[{}] SSE 연결 +1 (총 {}개)", stockId, emitters.get(stockId).size());

        // ③ 최초 구독 여부 판단
        if (subscribed.add(stockId)) {
            log.info("[{}] ▶️  KIS subscribe", stockId);
            kisClient.queueSubscribe(stockId, dto -> fanOut(stockId, dto));
        } else {
            log.debug("[{}] 이미 subscribe 중", stockId);
        }

        // ④ 종료 콜백 등록
        emitter.onTimeout(() -> handleDisconnect(stockId, emitter));
        emitter.onCompletion(() -> handleDisconnect(stockId, emitter));
        emitter.onError(e -> handleDisconnect(stockId, emitter));

        sendEvent(emitter, "message", "connected"); // ✅ 추가

        return emitter;
    }

    public void fanOut(String stockId, RealTimeTradeDTO dto) {
        List<SseEmitter> list = emitters.getOrDefault(stockId, new CopyOnWriteArrayList<>());

        for (SseEmitter emitter : list) {
            try {
                sendEvent(emitter, "trade", dto);
            } catch (Exception e) {
                log.debug("[{}] emitter 전송 실패, 제거", stockId);
                handleDisconnect(stockId, emitter);
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String name, Object payload) {
        try {
            Object body = payload;
            MediaType mt = MediaType.TEXT_PLAIN;

            if (payload instanceof Map || payload instanceof RealTimeTradeDTO) {
                body = om.writeValueAsString(payload);
                mt = MediaType.APPLICATION_JSON;
            }

            emitter.send(SseEmitter.event().name(name).data(body, mt));
        } catch (Exception e) {
            log.debug("sendEvent 실패 : {}", e.toString());
        }
    }

    private SseEmitter closedEmitter(String event, String msg) {
        SseEmitter emitter = new SseEmitter(0L);
        sendEvent(emitter, event, msg);
        safeComplete(emitter);
        return emitter;
    }

    private void handleDisconnect(String stockId, SseEmitter emitter) {
        ReentrantLock lock = locks.computeIfAbsent(stockId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<SseEmitter> list = emitters.get(stockId);
            if (list != null) {
                list.remove(emitter);

                if (list.isEmpty()) {
                    emitters.remove(stockId);
                    if (subscribed.remove(stockId)) {
                        log.info("[{}] ⏹️  마지막 구독자 종료 → KIS unsubscribe", stockId);
                        kisClient.queueUnsubscribe(stockId);
                    }
                }
            }
        } finally {
            lock.unlock();
        }

        safeComplete(emitter);
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (
                IllegalStateException ignored) {
            log.debug("Emitter already closed: {}", ignored.getMessage());
        } catch (Exception ex) {
            log.warn("SSE complete error", ex);
        }
    }
}
