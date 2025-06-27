package com.project.stock.investory.stockInfo.controller;

import com.project.stock.investory.stockInfo.service.StockWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockStreamController {
    private final StockWebSocketService service;

    /** Vue가 EventSource로 연결하는 엔드포인트 */
    @GetMapping("/{stockId}/stream")
    public SseEmitter stream(@PathVariable String stockId) throws Exception {
        return service.addSubscriber(stockId);
    }
}
