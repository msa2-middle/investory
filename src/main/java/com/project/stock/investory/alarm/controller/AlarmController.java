package com.project.stock.investory.alarm.controller;

import com.project.stock.investory.alarm.dto.AlarmResponseDTO;
import com.project.stock.investory.alarm.exception.AuthenticationRequiredException;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.alarm.service.RxSubjectManager;
import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
@Slf4j
public class AlarmController {

    private final AlarmService alarmService;
    private final RxSubjectManager rxSubjectManager;

    // 해당 유저가 가지고 있는 알람 가져오기 (기본 - 빠른 조회)
    @GetMapping("/storage")
    public ResponseEntity<List<AlarmResponseDTO>> findAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            // 비회원이면 빈 배열 반환
            return ResponseEntity.ok(List.of());
        }
        List<AlarmResponseDTO> alarms = alarmService.findAll(userDetails);
        return ResponseEntity.ok(alarms);
    }

    // 상세 정보를 포함한 알람 조회 (연관 엔티티 정보 포함)
    @GetMapping("/storage/details")
    public ResponseEntity<List<AlarmResponseDTO>> findAllWithDetails(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.ok(List.of());
        }
        List<AlarmResponseDTO> alarms = alarmService.findAllWithDetails(userDetails);
        return ResponseEntity.ok(alarms);
    }

    // 해당 유저에게 알람 보내기 (SSE)
    @GetMapping("/sse")
    public SseEmitter streamSse(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }
        log.info("SSE connection request from user: {}", userDetails.getUserId());
        return rxSubjectManager.createConnection(userDetails.getUserId());
    }

    // 로그아웃 시 subjectMap에서 제거
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.ok(Map.of("status", "unsubscribed"));
        }
        rxSubjectManager.unsubscribe(userDetails.getUserId());
        log.info("User unsubscribed: {}", userDetails.getUserId());
        return ResponseEntity.ok(Map.of("status", "unsubscribed"));
    }

    // 클라이언트 heartbeat 응답 처리
    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> handleHeartbeat(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.ok(Map.of("status", "pong"));
        }
        rxSubjectManager.updateUserHeartbeat(userDetails.getUserId());
        log.debug("Heartbeat received from user: {}", userDetails.getUserId());
        return ResponseEntity.ok(Map.of("status", "pong"));
    }

    // 유저의 알람 모두 읽음 표시
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> readAllAlarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "updatedCount", 0
            ));
        }
        int count = alarmService.readAllAlarm(userDetails);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "updatedCount", count
        ));
    }

    // 유저의 특정 알람 읽음 표시
    @PutMapping("/read-one/{alarmId}")
    public ResponseEntity<AlarmResponseDTO> readOneAlarm(
            @PathVariable Long alarmId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return ResponseEntity.ok(null);
        }
        AlarmResponseDTO dto = alarmService.readOneAlarm(userDetails, alarmId);
        return ResponseEntity.ok(dto);
    }

    // 관리자용 연결 상태 조회
    @GetMapping("/admin/connections")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        return ResponseEntity.ok(rxSubjectManager.getConnectionStats());
    }

    // 개발/테스트용 특정 사용자 연결 상태 조회
    @GetMapping("/debug/user/{userId}/connections")
    public ResponseEntity<Map<String, Object>> getUserConnectionInfo(@PathVariable Long userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "connectionCount", rxSubjectManager.getUserConnectionCount(userId),
                "activeSessions", rxSubjectManager.getActiveSessionsForUser(userId)
        ));
    }
}
