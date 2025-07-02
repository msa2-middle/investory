package com.project.stock.investory.alarm.controller;

import com.project.stock.investory.alarm.dto.AlarmResponseDTO;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    // 해당 유저가 가지고 있는 알람 가져오기 (기본 - 빠른 조회)
    @GetMapping("/storage")
    public ResponseEntity<List<AlarmResponseDTO>> findAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlarmResponseDTO> alarms = alarmService.findAll(userDetails);
        return ResponseEntity.ok(alarms);
    }

    // 상세 정보를 포함한 알람 조회 (연관 엔티티 정보 포함)
    @GetMapping("/storage/details")
    public ResponseEntity<List<AlarmResponseDTO>> findAllWithDetails(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlarmResponseDTO> alarms = alarmService.findAllWithDetails(userDetails);
        return ResponseEntity.ok(alarms);
    }

    // 해당 유저에게 알람 보내기 (SSE)
    @GetMapping("/sse")
    public SseEmitter streamSse(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SseEmitter emitter = new SseEmitter(60*60*1000L); // 1시간 타임아웃

        alarmService.subscribe(userDetails).subscribe(alarm -> {
            try {
                emitter.send(SseEmitter.event().name("alarm").data(alarm));
            } catch (Exception e) {
                emitter.completeWithError(e);
                // 연결 끊어지면 구독 해제
                alarmService.unsubscribe(userDetails);
            }
        });

        emitter.onCompletion(() -> alarmService.unsubscribe(userDetails));
        emitter.onTimeout(() -> alarmService.unsubscribe(userDetails));
        emitter.onError(e -> alarmService.unsubscribe(userDetails));

        return emitter;
    }

    // 로그아웃 시 subjectMap에서 제거
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alarmService.unsubscribe(userDetails);
        return ResponseEntity.ok(Map.of("status", "unsubscribed"));
    }

    // 유저의 알람 모두 읽음 표시
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> readAllAlarm(@AuthenticationPrincipal CustomUserDetails userDetails) {
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
        AlarmResponseDTO dto = alarmService.readOneAlarm(userDetails, alarmId);
        return ResponseEntity.ok(dto);
    }
}