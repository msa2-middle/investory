package com.project.stock.investory.stockInfo.service;


import com.project.stock.investory.stockInfo.util.StockMarketUtils;
import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class StockWebSocketService {
    private final KisWebSocketClient kisClient;

    // 활성 SSE 연결 관리
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public StockWebSocketService(KisWebSocketClient kisClient) {
        this.kisClient = kisClient;
    }

    /**
     * 특정 종목의 실시간 가격 정보를 SSE로 스트리밍
     */
    public SseEmitter getStockPriceStream(String stockId) {
        // 국내장 시간
        if (!StockMarketUtils.isTradingHours()) {
            log.info("장 외 시간, SSE 연결 차단: {}", stockId);
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("marketClosed").data("장 외 시간입니다."));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // 기존 연결이 있으면 정리
        SseEmitter existingEmitter = activeEmitters.get(stockId);
        if (existingEmitter != null) {
            try {
                existingEmitter.complete();
            } catch (Exception e) {
                log.debug("기존 emitter 정리 중 오류", e);
            }
        }

        // 새 SSE 연결 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        activeEmitters.put(stockId, emitter);

        log.info("종목 {} 실시간 가격 스트리밍 시작", stockId);

        // 연결 성공 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("종목 " + stockId + " 실시간 데이터 연결됨", MediaType.TEXT_PLAIN));
        } catch (IOException | java.io.IOException e) {
            log.error("초기 연결 메시지 전송 실패: {}", stockId, e);
            cleanupEmitter(stockId, emitter);
            emitter.completeWithError(e);
            return emitter;
        }

        // KIS WebSocket에서 해당 종목 데이터를 받아서 SSE로 전송하는 핸들러
        kisClient.startListening(stockId, dto -> {
            SseEmitter currentEmitter = activeEmitters.get(stockId);
            if (currentEmitter == null || !currentEmitter.equals(emitter)) {
                // 이미 다른 emitter로 교체됨
                return;
            }

            try {
                currentEmitter.send(SseEmitter.event()
                        .name("priceUpdate")
                        .data(dto));
            } catch (IOException ex) {
                log.debug("SSE 데이터 전송 실패 (연결 끊김): {}", stockId);
                cleanupEmitter(stockId, currentEmitter);
                kisClient.stopListening(stockId);
            } catch (IllegalStateException ex) {
                log.debug("SSE emitter 상태 오류: {}", stockId);
                cleanupEmitter(stockId, currentEmitter);
                kisClient.stopListening(stockId);
            } catch (Exception ex) {
                log.error("SSE 데이터 전송 중 예상치 못한 오류: {}", stockId, ex);
                cleanupEmitter(stockId, currentEmitter);
                kisClient.stopListening(stockId);
            }
        });

        // SSE 연결 이벤트 핸들러 설정
        emitter.onCompletion(() -> {
            log.info("종목 {} SSE 연결 완료", stockId);
            cleanupEmitter(stockId, emitter);
            kisClient.stopListening(stockId);
        });

        emitter.onTimeout(() -> {
            log.info("종목 {} SSE 연결 타임아웃", stockId);
            cleanupEmitter(stockId, emitter);
            kisClient.stopListening(stockId);
        });

        emitter.onError((throwable) -> {
            if (isConnectionResetError(throwable)) {
                log.debug("종목 {} 클라이언트 연결 종료", stockId);
            } else {
                log.warn("종목 {} SSE 연결 오류", stockId, throwable);
            }
            cleanupEmitter(stockId, emitter);
            kisClient.stopListening(stockId);
        });

        return emitter;
    }

    /**
     * 연결 리셋 오류인지 확인
     */
    private boolean isConnectionResetError(Throwable throwable) {
        if (throwable == null) return false;

        String message = throwable.getMessage();
        return message != null && (
                message.contains("Connection reset") ||
                        message.contains("Broken pipe") ||
                        message.contains("현재 연결은 사용자의 호스트 시스템의 소프트웨어의 의해 중단되었습니다") ||
                        message.contains("An existing connection was forcibly closed")
        );
    }

    /**
     * Emitter 정리
     */
    private void cleanupEmitter(String stockId, SseEmitter emitter) {
        SseEmitter currentEmitter = activeEmitters.get(stockId);
        if (currentEmitter == emitter) {
            activeEmitters.remove(stockId);
        }

        try {
            if (!emitter.equals(currentEmitter)) {
                emitter.complete();
            }
        } catch (Exception e) {
            log.debug("Emitter 정리 중 오류", e);
        }
    }

    /**
     * 모든 활성 연결 종료 (애플리케이션 종료 시)
     */
    public void shutdown() {
        log.info("모든 SSE 연결 종료");
        for (Map.Entry<String, SseEmitter> entry : activeEmitters.entrySet()) {
            try {
                entry.getValue().complete();
            } catch (Exception e) {
                log.debug("SSE 연결 종료 중 오류: {}", entry.getKey(), e);
            }
        }
        activeEmitters.clear();
    }
}
