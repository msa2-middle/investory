package com.project.stock.investory.stockInfo.service;

import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class StockWebSocketService {

    private final KisWebSocketClient kis;
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public StockWebSocketService(KisWebSocketClient kis) { this.kis = kis; }

    public SseEmitter addSubscriber(String stockId) throws Exception {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(stockId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 끊긴 emitter 정리
        Runnable cleanup = () -> emitters.get(stockId).remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // 아직 KIS에 구독 안되어 있으면 추가
        kis.subscribe(stockId, this::broadcast);

        return emitter;
    }

    /* Kis 콜백 → 같은 종목 구독자에게 전파 */
    private void broadcast(RealTimeTradeDTO dto) {
        CopyOnWriteArrayList<SseEmitter> list =
                emitters.getOrDefault(dto.getStockId(), new CopyOnWriteArrayList<>());

        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name("trade").data(dto));
            } catch (IOException ex) {
                list.remove(e);   // 리스트가 실제로 map 에 있는 객체일 때만 제거됨
            }
        }
    }
}
