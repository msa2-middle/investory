package com.project.stock.investory.stockInfo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import com.project.stock.investory.stockInfo.exception.StockNotFoundException;
import com.project.stock.investory.stockInfo.repository.StockRepository;
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

/**
 * 주식 실시간 체결 데이터를 KIS WebSocket 으로부터 받아
 * 구독 중인 클라이언트(SSE) 들에게 전달(fan‑out)하는 서비스.
 * 주요 책임
 *
 *   1. KIS 구독/해지 관리
 *   2. SSE 연결(Emitter) 생성·보존·해제
 *   3. 실시간 체결 데이터 fan‑out(전달)
 *   4. 다중 스레드 환경에서 안전한 동시성 제어
 */
@Slf4j
@Service
public class StockWebSocketService {

    private final KisWebSocketClient kisClient;
    private final StockRepository stockRepository;
    private final ObjectMapper om = new ObjectMapper();

    //여러 스레드가 동시에 읽고 쓸 수 있게 하여 동시성 문제를 방지 : ConcurrentHashMap<>()
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet(); // DTO 객체나 Map을 JSON 문자열로 변환해 SSE로 전송
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>(); // 종목 코드(stockId)별로 구독 중인 SSE 연결들을 저장

    public StockWebSocketService(KisWebSocketClient kisClient, StockRepository stockRepository) {
        this.kisClient = kisClient;
        this.stockRepository=stockRepository;
    }

    public SseEmitter getStockPriceStream(String stockId) {

        // ① 장 외 시간이면 종료
        if (!StockMarketUtils.isTradingHours()) {
            return closedEmitter("marketClosed", "장 외 시간입니다.");
        }

        // 해당 종목 번호(stockId)가 있는지,, 없으면 StockNotFoundException 호출
        if(!stockRepository.existsByStockId((stockId))){
            throw new StockNotFoundException(stockId);
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
        emitter.onError(e -> removeEmitterOnly(stockId, emitter));

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

    /**
     * 즉시 종료되는 Emitter (장이 닫혀있을 때 등) 생성 유틸리티.
     */
    private SseEmitter closedEmitter(String event, String msg) {
        SseEmitter emitter = new SseEmitter(0L);
        sendEvent(emitter, event, msg);
        safeComplete(emitter);
        return emitter;
    }

    /**
     * onError 상황에서 emitter.complete() 는 호출하지 않고 목록 정리만 수행.
     */
    private void removeEmitterOnly(String stockId, SseEmitter emitter) {
        ReentrantLock lock = locks.computeIfAbsent(stockId, k -> new ReentrantLock());
        lock.lock();
        try {
            List<SseEmitter> list = emitters.get(stockId);
            if (list != null) {
                list.remove(emitter);

                // 해당 종목을 구독 중인 Emitter 가 더 이상 없으면 자원 정리 & KIS 구독 해지
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
        // emitter.complete() 호출 없음 → AsyncRequestNotUsableException 방지
    }

    /**
     * timeout 또는 completion 이벤트에서 호출되어 emitter.complete() 까지 수행.
     */
    private void handleDisconnect(String stockId, SseEmitter emitter) {
        removeEmitterOnly(stockId, emitter); // 목록 정리 재사용
        safeComplete(emitter);              // complete() 호출은 여기서만
    }

    /**
     * Emitter 를 안전하게 complete 처리. 이미 종료된 Emitter 에서 발생할 수 있는 예외를 흡수한다.
     */
    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            log.debug("Emitter unusable: {}", ignored.getMessage());
        } catch (Exception ex) {
            log.warn("SSE complete error", ex);
        }
    }
}