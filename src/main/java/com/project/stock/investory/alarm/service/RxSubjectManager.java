package com.project.stock.investory.alarm.service;

import com.project.stock.investory.alarm.dto.AlarmResponseDTO;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RxSubjectManager {

    private final Map<Long, PublishSubject<AlarmResponseDTO>> subjectMap = new ConcurrentHashMap<>();

    public Observable<AlarmResponseDTO> getObservableForUser(Long userId) {
        return subjectMap
                .computeIfAbsent(userId, id -> PublishSubject.create())
                .hide();
    }

    public void emit(Long userId, AlarmResponseDTO alarmResponseDTO) {
        PublishSubject<AlarmResponseDTO> subject = subjectMap.get(userId);
        if (subject != null) {
            if (subject.hasObservers()) {
                subject.onNext(alarmResponseDTO);
            } else {
                // Observer가 없으면 정리
                subjectMap.remove(userId);
            }
        }
    }

    // 로그아웃 시 subjectMap에서 제거
    public void unsubscribe(Long userId) {
        PublishSubject<AlarmResponseDTO> subject = subjectMap.get(userId);
        if (subject != null) {
            subject.onComplete(); // 스트림 완료
            subjectMap.remove(userId);
        }
    }
}