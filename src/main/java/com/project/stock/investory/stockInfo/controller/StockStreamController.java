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
 * 📡 SSE(Server-Sent Events) 실시간 스트리밍 컨트롤러
 *
 * ===================================================================
 * 🏆 자소서 핵심 어필 포인트: WebSocket + SSE 하이브리드 아키텍처
 * ===================================================================
 *
 * 💡 이 컨트롤러의 핵심 역할:
 * 🔄 KIS WebSocket API → 내부 SSE 스트리밍 → 웹 클라이언트
 *
 * 1️⃣ 【기술적 복잡도】프로토콜 변환 계층
 *    ✅ 외부: WebSocket (KIS API 제약)
 *    ✅ 내부: SSE (웹 클라이언트 최적화)
 *    ✅ 중간: 실시간 데이터 파이프라인 구축
 *
 * 2️⃣ 【실무 적용성】사용자 경험 최적화
 *    ✅ 자동 재연결: EventSource API 내장 기능 활용
 *    ✅ 이벤트 기반: trade, connected 등 의미있는 이벤트 분리
 *    ✅ 실시간 UI: 브라우저 네이티브 SSE 지원
 *
 * 3️⃣ 【아키텍처 설계】계층 분리와 책임 분산
 *    ✅ Controller: HTTP 요청/응답, SSE 연결 관리
 *    ✅ Service: WebSocket 구독, 데이터 변환
 *    ✅ Client: KIS API 연동, 재연결 로직
 *
 * 📊 구현 특징:
 * - MediaType.TEXT_EVENT_STREAM_VALUE로 SSE 응답 타입 명시
 * - SseEmitter 30분 타임아웃으로 메모리 누수 방지
 * - JavaScript EventSource API와 완벽 호환
 * - 실시간 주가 데이터 JSON 직렬화 및 전송
 */
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j
public class StockStreamController {

    // 의존성 주입: 실제 비즈니스 로직을 처리하는 서비스
    private final StockWebSocketService service;

    /**
     * 📈 실시간 주식 가격 스트리밍 엔드포인트 - 하이브리드 아키텍처의 핵심!
     *
     * ===================================================================
     * 🏆 자소서 어필 포인트: 1:N 팬아웃 최적화 + 프로토콜 변환
     * ===================================================================
     *
     * 🔍 이 메서드가 하는 일:
     * 1. HTTP 요청을 받아서 SSE 스트림 연결로 업그레이드
     * 2. 백그라운드에서 WebSocket 구독 시작 (내부적으로)
     * 3. 실시간 데이터가 오면 SSE 이벤트로 변환해서 전송
     *
     * 🎯 기술적 복잡도:
     * - HTTP → SSE 프로토콜 변환
     * - MediaType.TEXT_EVENT_STREAM_VALUE 헤더 설정
     * - 장시간 연결 유지를 위한 Keep-Alive 설정
     *
     * 🚀 성능 최적화:
     * - 동일 종목 중복 구독 방지 (서비스 레이어에서 처리)
     * - 메모리 효율적 SseEmitter 관리
     * - 자동 타임아웃으로 좀비 연결 방지
     *
     * @param stockId 종목코드 (예: "005930" = 삼성전자)
     * @return SseEmitter 실시간 스트림 연결 객체
     *
     * 🔄 전체 데이터 플로우:
     * 클라이언트 → HTTP 요청 → 이 메서드 → Service → WebSocket Client → KIS API
     *            ←── SSE 스트림 ←── Service ←── 실시간 데이터 ←── KIS API
     */
    @GetMapping(value = "/{stockId}/realTimeprice", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> getStockPrice(@PathVariable String stockId) {

        log.info("📊 [{}] 실시간 스트림 요청 - SSE 연결 시작", stockId);

        // 🎯 핵심: 서비스 레이어에서 복잡한 로직 처리
        // 1. SseEmitter 생성 (30분 타임아웃)
        // 2. WebSocket 구독 요청 (중복 방지 로직 포함)
        // 3. 실시간 데이터 → SSE 이벤트 변환 콜백 등록
        SseEmitter emitter = service.getStockPriceStream(stockId);

        // 🌐 SSE 응답 헤더 설정 - 브라우저 호환성 최적화
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8") // SSE 필수 헤더
                .header(HttpHeaders.CACHE_CONTROL, "no-cache") // 캐싱 방지
                .header(HttpHeaders.CONNECTION, "keep-alive") // 장시간 연결 유지
                .body(emitter); // 실시간 스트림 연결 객체 반환
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