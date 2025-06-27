package com.project.stock.investory.stockInfo.service;


import com.project.stock.investory.stockInfo.dto.RealTimeTradeDTO;
import com.project.stock.investory.stockInfo.websocket.KisWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@Slf4j
public class StockWebSocketService {
    private final KisWebSocketClient kisClient;

    public StockWebSocketService(KisWebSocketClient kisClient) {
        this.kisClient = kisClient;
    }

    /**
     * 특정 종목의 실시간 가격 정보를 SSE로 스트리밍
     */
    public SseEmitter getStockPriceStream(String stockId) {
        // SSE 연결 생성 (무제한 타임아웃)
//        SseEmitter emitter = new SseEmitter(0L);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30분


        log.info("종목 {} 실시간 가격 스트리밍 시작", stockId);

        // 연결 성공 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("종목 " + stockId + " 실시간 데이터 연결됨",
                            MediaType.TEXT_PLAIN)) ;  // charset=UTF-8 로 직렬화));
        } catch (IOException e) {
            log.error("초기 연결 메시지 전송 실패", e);
            emitter.completeWithError(e);
            return emitter;
        }

        // KIS WebSocket에서 해당 종목 데이터를 받아서 SSE로 전송하는 핸들러
        kisClient.startListening(stockId, dto -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("priceUpdate")
                        .data(dto));
            } catch (IOException ex) {
                if (log.isDebugEnabled()) log.debug("SSE 끊김: {}", stockId);
                kisClient.stopListening(stockId);
                emitter.complete();
            }
        });

        // SSE 연결이 끊어졌을 때 정리 작업
        emitter.onCompletion(() -> {
            log.info("종목 {} SSE 연결 완료", stockId);
            kisClient.stopListening(stockId);
        });

        emitter.onTimeout(() -> {
            log.info("종목 {} SSE 연결 타임아웃", stockId);
            kisClient.stopListening(stockId);
        });

        emitter.onError((throwable) -> {
            log.warn("종목 {} SSE 연결 오류", stockId, throwable);
            kisClient.stopListening(stockId);
        });

        return emitter;
    }


}