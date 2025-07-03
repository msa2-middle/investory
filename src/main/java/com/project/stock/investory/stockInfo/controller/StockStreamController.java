package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.service.StockWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE(Server-Sent Events) 컨트롤러
 *
 * 역할:
 * 1. 클라이언트의 실시간 주식 데이터 요청 받기
 * 2. SseEmitter 객체 생성하여 클라이언트와 연결
 * 3. 테스트용 HTML 페이지 제공
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j
public class StockStreamController {

    // 의존성 주입: 실제 비즈니스 로직을 처리하는 서비스
    private final StockWebSocketService service;

    /**
     * 실시간 주식 가격 스트리밍 엔드포인트
     *
     * @param stockId 종목코드 (예: "005930" - 삼성전자)
     * @return SseEmitter 객체 (클라이언트와의 실시간 연결)
     *
     * 동작 과정:
     * 1. 클라이언트가 GET /stock/005930/realTimeprice 요청
     * 2. 서비스에서 SseEmitter 생성 및 WebSocket 구독 처리
     * 3. 클라이언트에게 SseEmitter 반환 (실시간 연결 수립)
     */
    @GetMapping(value = "/{stockId}/realTimeprice", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> getStockPrice(@PathVariable String stockId) {

        log.info("📊 실시간 주식 가격 요청: {}", stockId);

        // 핵심: 서비스에게 실시간 데이터 스트림 요청
        // 이 메서드 안에서 WebSocket 구독, SSE 연결 생성 등이 모두 처리됨
        SseEmitter emitter = service.getStockPriceStream(stockId);

        // SSE 응답 헤더 설정
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8") // SSE 필수 헤더
                .body(emitter); // 실시간 연결 객체 반환
    }

    /**
     * 테스트용 HTML 페이지 제공
     *
     * 브라우저에서 /stock/005930 접속 시 실시간 주가를 볼 수 있는 페이지 제공
     * JavaScript EventSource API를 사용하여 SSE 연결 구현
     */
    @GetMapping("/{stockId}")
    public String getStockPage(@PathVariable String stockId) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>실시간 주식 가격 - %s</title>
            <meta charset="UTF-8">
        </head>
        <body>
            <h1>종목코드: %s 실시간 가격</h1>
            <div id="stockInfo">
                <p>🔄 연결 중...</p>
            </div>
            
            <script>
                console.log('🚀 SSE 클라이언트 시작');
                
                // EventSource: 브라우저 내장 SSE 클라이언트
                // 자동 재연결, 이벤트 기반 처리 제공
                const eventSource = new EventSource('/stock/%s/realTimeprice');
                const stockInfoDiv = document.getElementById('stockInfo');
                
                // 연결 수립 이벤트
                eventSource.onopen = function() {
                    console.log('✅ SSE 연결 성공');
                    stockInfoDiv.innerHTML = '<p style="color: blue;">🔗 서버와 연결됨</p>';
                };
                
                // 'connected' 이벤트 리스너 (서버에서 연결 확인 메시지)
                eventSource.addEventListener('connected', function(event) {
                    console.log('📡 연결 확인:', event.data);
                    stockInfoDiv.innerHTML = '<p style="color: green;">✅ ' + event.data + '</p>';
                });
                
                // 'trade' 이벤트 리스너 (실시간 체결 데이터)
                eventSource.addEventListener('trade', function(event) {
                    console.log('📈 실시간 데이터:', event.data);
                    
                    // JSON 파싱하여 화면에 표시
                    const data = JSON.parse(event.data);
                    stockInfoDiv.innerHTML = `
                        <div style="border: 2px solid #2196F3; padding: 15px; margin: 10px; border-radius: 8px; background: #f5f5f5;">
                            <h3>📊 ${data.stockId} 실시간 정보</h3>
                            <p><strong>💰 현재가:</strong> <span style="font-size: 1.2em; color: #2196F3;">${data.tradePrice.toLocaleString()}원</span></p>
                            <p><strong>📊 등락율:</strong> <span style="color: ${data.changeRate > 0 ? 'red' : 'blue'};">${data.changeRate}%</span></p>
                            <p><strong>📦 체결량:</strong> ${data.tradeVolume.toLocaleString()}</p>
                            <p><strong>📈 누적거래량:</strong> ${data.accumulateVolume.toLocaleString()}</p>
                            <p><strong>⏰ 체결시간:</strong> ${data.tradeTime}</p>
                            <p><small>🕐 업데이트: ${new Date().toLocaleTimeString()}</small></p>
                        </div>
                    `;
                });
                
                // 에러 처리
                eventSource.onerror = function(event) {
                    console.error('❌ SSE 연결 오류:', event);
                    stockInfoDiv.innerHTML = '<p style="color: red;">❌ 연결 오류 발생</p>';
                    
                    // EventSource는 자동으로 재연결을 시도함
                    // 별도 처리 없이도 3초 후 자동 재연결
                };
                
                // 페이지 종료 시 연결 정리 (메모리 누수 방지)
                window.addEventListener('beforeunload', function() {
                    console.log('🔌 연결 종료');
                    eventSource.close();
                });
            </script>
        </body>
        </html>
        """.formatted(stockId, stockId, stockId);
    }
}