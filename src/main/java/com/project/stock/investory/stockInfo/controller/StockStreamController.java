package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.service.StockWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
@Slf4j

public class StockStreamController {

    private final StockWebSocketService service;

    /**
     * 실시간 주식 가격 스트리밍
     * 사용법: GET /stock/{stockId}/price
     * 예: GET /stock/005930/price (삼성전자)
     */
    @GetMapping("/{stockId}/realTimeprice")
    public ResponseEntity<SseEmitter> getStockPrice(@PathVariable String stockId) {

        log.info("실시간 주식 가격 요청: {}", stockId);
        SseEmitter emitter = service.getStockPriceStream(stockId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8") // ← 강제
                .body(emitter);
    }

    /**
     * 웹페이지에서 사용할 HTML (테스트용)
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
                <p>연결 중...</p>
            </div>
            
            <script>
                // SSE 연결
                const eventSource = new EventSource('/stock/%s/price');
                const stockInfoDiv = document.getElementById('stockInfo');
                
                eventSource.onopen = function() {
                    console.log('SSE 연결 성공');
                };
                
                eventSource.addEventListener('connected', function(event) {
                    stockInfoDiv.innerHTML = '<p style="color: green;">' + event.data + '</p>';
                });
                
                eventSource.addEventListener('priceUpdate', function(event) {
                    const data = JSON.parse(event.data);
                    stockInfoDiv.innerHTML = `
                        <div style="border: 1px solid #ccc; padding: 10px; margin: 5px;">
                            <h3>종목: ${data.stockId}</h3>
                            <p><strong>현재가:</strong> ${data.tradePrice}원</p>
                            <p><strong>등락율:</strong> ${data.changeRate}</p>
                            <p><strong>체결량:</strong> ${data.tradeVolume}</p>
                            <p><strong>누적거래량:</strong> ${data.accumulateVolume}</p>
                            <p><strong>체결시간:</strong> ${data.tradeTime}</p>
                        </div>
                    `;
                });
                
                eventSource.onerror = function(event) {
                    console.error('SSE 연결 오류:', event);
                    stockInfoDiv.innerHTML = '<p style="color: red;">연결 오류 발생</p>';
                };
                
                // 페이지 떠날 때 연결 정리
                window.addEventListener('beforeunload', function() {
                    eventSource.close();
                });
            </script>
        </body>
        </html>
        """.formatted(stockId, stockId, stockId);
    }
}
