package com.project.stock.investory.alarm.service;

import com.project.stock.investory.alarm.entity.Alarm;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RxSubjectManager {

    private final Map<Long, PublishSubject<Alarm>> subjectMap = new ConcurrentHashMap<>();

    public Observable<Alarm> getObservableForUser(Long userId) {

        return subjectMap
                .computeIfAbsent(userId, id -> PublishSubject.create())
                .hide();
    }

    public void emit(Long userId, Alarm alarm) {
        PublishSubject<Alarm> subject = subjectMap.get(userId);
        if (subject != null) {
            if (subject.hasObservers()) {
                subject.onNext(alarm);
            } else {
                // Observer가 없으면 정리
                subjectMap.remove(userId);
            }
        }
    }

    // 로그아웃 시 subjectMap에서 제거
    public void unsubscribe(Long userId) {
        subjectMap.remove(userId);
    }

}