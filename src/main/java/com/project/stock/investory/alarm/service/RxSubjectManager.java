package com.project.stock.investory.alarm.service;

import com.project.stock.investory.alarm.dto.AlarmResponseDTO;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 개선된 RxSubjectManager - 기존 메서드명 유지
 * - 메모리 누수 방지
 * - Heartbeat 시스템
 * - 연결 상태 추적
 * - 자동 정리 기능
 */
@Component
@Slf4j
public class RxSubjectManager {

    // 기존 RxJava Subject 관리 (호환성 유지)
    private final Map<Long, PublishSubject<AlarmResponseDTO>> subjectMap = new ConcurrentHashMap<>();

    // 새로 추가된 SSE 연결 관리
    private final Map<String, SseConnection> connections = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userConnections = new ConcurrentHashMap<>();

    // 설정값
    private static final long TIMEOUT_MINUTES = 30;
    private static final Duration CONNECTION_TIMEOUT = Duration.ofMinutes(TIMEOUT_MINUTES);

    // === 기존 메서드들 (호환성 유지) ===

    /**
     * 기존 메서드 - Observable 반환
     */
    public Observable<AlarmResponseDTO> getObservableForUser(Long userId) {
        return subjectMap
                .computeIfAbsent(userId, id -> PublishSubject.create())
                .hide();
    }

    /**
     * 기존 메서드 - 알람 전송
     */
    public void emit(Long userId, AlarmResponseDTO alarmResponseDTO) {
        PublishSubject<AlarmResponseDTO> subject = subjectMap.get(userId);
        if (subject != null) {
            if (subject.hasObservers()) {
                subject.onNext(alarmResponseDTO);
                log.debug("Alarm emitted to user: {}", userId);
            } else {
                // Observer가 없으면 정리
                subjectMap.remove(userId);
                log.debug("No observers for user: {}, subject removed", userId);
            }
        }
    }

    /**
     * 기존 메서드 - 구독 해제
     */
    public void unsubscribe(Long userId) {
        // 기존 RxJava Subject 정리
        PublishSubject<AlarmResponseDTO> subject = subjectMap.get(userId);
        if (subject != null) {
            subject.onComplete();
            subjectMap.remove(userId);
        }

        // 새로 추가된 SSE 연결도 정리
        removeUserConnections(userId);

        log.info("User unsubscribed: {}", userId);
    }

    // === 새로 추가된 SSE 관리 메서드들 ===

    /**
     * 30초마다 모든 활성 연결에 heartbeat 전송
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        connections.values().forEach(connection -> {
            if (connection.isActive()) {
                try {
                    connection.getEmitter().send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                    log.debug("Heartbeat sent to user: {}", connection.getUserId());
                } catch (Exception e) {
                    log.warn("Heartbeat failed for user: {}, removing connection",
                            connection.getUserId(), e);
                    removeConnection(connection.getSessionId());
                }
            }
        });
    }

    /**
     * 1분마다 만료된 연결 정리
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredConnections() {
        List<String> expiredSessions = connections.values().stream()
                .filter(conn -> conn.isExpired(CONNECTION_TIMEOUT))
                .map(SseConnection::getSessionId)
                .collect(Collectors.toList());

        expiredSessions.forEach(sessionId -> {
            log.info("Removing expired connection: {}", sessionId);
            removeConnection(sessionId);
        });

        if (!expiredSessions.isEmpty()) {
            log.debug("Active connections: {}, Cleaned up: {}",
                    connections.size(), expiredSessions.size());
        }
    }

    /**
     * 새로운 SSE 연결 생성
     */
    public SseEmitter createConnection(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(TIMEOUT_MINUTES * 60 * 1000L);

        SseConnection connection = new SseConnection(
                userId, emitter, LocalDateTime.now(),
                LocalDateTime.now(), sessionId, true
        );

        // 연결 저장
        connections.put(sessionId, connection);
        userConnections.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        // 이벤트 핸들러 설정
        setupEmitterHandlers(emitter, sessionId, userId);

        // RxJava 구독 설정 (기존 방식과 연동)
        setupRxSubscription(userId, sessionId);

        log.info("SSE connection created - User: {}, Session: {}", userId, sessionId);

        // 연결 확인 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("sessionId", sessionId, "userId", userId)));
        } catch (Exception e) {
            log.error("Failed to send connection confirmation", e);
            removeConnection(sessionId);
            throw new RuntimeException("SSE connection setup failed", e);
        }

        return emitter;
    }

    /**
     * SseEmitter 이벤트 핸들러 설정
     */
    private void setupEmitterHandlers(SseEmitter emitter, String sessionId, Long userId) {
        emitter.onCompletion(() -> {
            log.info("SSE connection completed - User: {}, Session: {}", userId, sessionId);
            removeConnection(sessionId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timed out - User: {}, Session: {}", userId, sessionId);
            removeConnection(sessionId);
        });

        emitter.onError(throwable -> {
            log.error("SSE connection error - User: {}, Session: {}",
                    userId, sessionId, throwable);
            removeConnection(sessionId);
        });
    }

    /**
     * RxJava 구독 설정 (기존 방식과 연동)
     */
    private void setupRxSubscription(Long userId, String sessionId) {
        PublishSubject<AlarmResponseDTO> subject = subjectMap
                .computeIfAbsent(userId, k -> PublishSubject.create());

        subject.subscribe(
                alarm -> {
                    SseConnection connection = connections.get(sessionId);
                    if (connection != null && connection.isActive()) {
                        try {
                            connection.getEmitter().send(SseEmitter.event()
                                    .name("alarm")
                                    .data(alarm));
                            connection.updateHeartbeat();
                            log.debug("Alarm sent to user: {}, session: {}", userId, sessionId);
                        } catch (Exception e) {
                            log.error("Failed to send alarm to user: {}, session: {}",
                                    userId, sessionId, e);
                            removeConnection(sessionId);
                        }
                    }
                },
                error -> {
                    log.error("RxJava error for user: {}, session: {}",
                            userId, sessionId, error);
                    removeConnection(sessionId);
                },
                () -> {
                    log.info("RxJava stream completed for user: {}, session: {}",
                            userId, sessionId);
                    removeConnection(sessionId);
                }
        );
    }

    /**
     * 특정 연결 제거
     */
    public void removeConnection(String sessionId) {
        SseConnection connection = connections.remove(sessionId);
        if (connection != null) {
            connection.markInactive();

            // 사용자별 연결 목록에서 제거
            Set<String> userSessions = userConnections.get(connection.getUserId());
            if (userSessions != null) {
                userSessions.remove(sessionId);
                if (userSessions.isEmpty()) {
                    userConnections.remove(connection.getUserId());
                }
            }

            // Emitter 정리
            try {
                connection.getEmitter().complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for session: {}", sessionId, e);
            }

            log.info("SSE connection removed - User: {}, Session: {}",
                    connection.getUserId(), sessionId);
        }
    }

    /**
     * 특정 사용자의 모든 연결 제거
     */
    public void removeUserConnections(Long userId) {
        Set<String> sessions = userConnections.get(userId);
        if (sessions != null) {
            // 복사본 생성 (ConcurrentModificationException 방지)
            Set<String> sessionsToRemove = new HashSet<>(sessions);
            sessionsToRemove.forEach(this::removeConnection);
        }
    }

    /**
     * 특정 사용자의 heartbeat 업데이트
     */
    public void updateUserHeartbeat(Long userId) {
        Set<String> sessions = userConnections.get(userId);
        if (sessions != null) {
            sessions.forEach(sessionId -> {
                SseConnection connection = connections.get(sessionId);
                if (connection != null) {
                    connection.updateHeartbeat();
                }
            });
        }
    }

    // === 연결 상태 조회 메서드들 ===

    public int getActiveConnectionCount() {
        return (int) connections.values().stream()
                .filter(SseConnection::isActive)
                .count();
    }

    public int getUserConnectionCount(Long userId) {
        Set<String> sessions = userConnections.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    public List<String> getActiveSessionsForUser(Long userId) {
        Set<String> sessions = userConnections.get(userId);
        if (sessions == null) return Collections.emptyList();

        return sessions.stream()
                .filter(sessionId -> {
                    SseConnection conn = connections.get(sessionId);
                    return conn != null && conn.isActive();
                })
                .collect(Collectors.toList());
    }

    /**
     * 관리자용 연결 정보 조회
     */
    public Map<String, Object> getConnectionStats() {
        return Map.of(
                "totalConnections", connections.size(),
                "activeConnections", getActiveConnectionCount(),
                "connectedUsers", userConnections.size(),
                "rxSubjects", subjectMap.size()
        );
    }
}