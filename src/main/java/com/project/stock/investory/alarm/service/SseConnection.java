package com.project.stock.investory.alarm.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * SSE 연결 정보를 관리하는 클래스
 */
@Getter
@AllArgsConstructor
public class SseConnection {
    private final Long userId;
    private final SseEmitter emitter;
    private final LocalDateTime connectedAt;
    private LocalDateTime lastHeartbeat;
    private final String sessionId;
    private boolean isActive;

    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    public void markInactive() {
        this.isActive = false;
    }

    public boolean isExpired(Duration timeout) {
        return Duration.between(lastHeartbeat, LocalDateTime.now()).compareTo(timeout) > 0;
    }
}