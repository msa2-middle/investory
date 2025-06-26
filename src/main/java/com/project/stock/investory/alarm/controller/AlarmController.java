package com.project.stock.investory.alarm.controller;


import com.project.stock.investory.alarm.dto.AlarmResponseDTO;
import com.project.stock.investory.alarm.entity.Alarm;
import com.project.stock.investory.alarm.service.AlarmService;

import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

//@Tag(name = "alarm controller api", description = "알람에 사용되는 API")
@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    // 해당 유저가 가지고 있는 알람 가져오기
    @GetMapping("/storage")
    public ResponseEntity<List<Alarm>> findAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Alarm> alarms = alarmService.findAll(userDetails);
        return ResponseEntity.ok(alarms); // 명시적으로 JSON 응답
    }

    // 해당 유저에게 알람 보내기
    @GetMapping("/sse")
    public SseEmitter streamSse(@AuthenticationPrincipal CustomUserDetails userDetails) {
        SseEmitter emitter = new SseEmitter(1000L); // 5분 타임아웃

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
    @DeleteMapping("/unsubscribe/{userId}")
    public void unsubscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alarmService.unsubscribe(userDetails);
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

    // 유저의 알람 읽음 표시
    @PutMapping("/read-one/{alarmId}")
    public ResponseEntity<AlarmResponseDTO> readOneAlarm(
            @PathVariable Long alarmId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AlarmResponseDTO dto = alarmService.readOneAlarm(userDetails, alarmId);
        return ResponseEntity.ok(dto);
    }
}