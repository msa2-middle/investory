package com.project.stock.investory.alarm.service;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.dto.AlarmResponseDTO;
import com.project.stock.investory.alarm.entity.Alarm;
import com.project.stock.investory.alarm.exception.AlarmNotFoundException;
import com.project.stock.investory.alarm.exception.UserNotFoundException;
import com.project.stock.investory.alarm.repository.AlarmRepository;
import com.project.stock.investory.alarm.exception.AuthenticationRequiredException;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import io.reactivex.rxjava3.core.Observable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class  AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final RxSubjectManager rxSubjectManager;

    // 알람 생성하기
    // todo: 저장할 때 userId만 넘겨주기
    @Transactional
    public Alarm createAlarm(AlarmRequestDTO dto, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(()->new UserNotFoundException());

        Alarm alarm = Alarm.builder()
                .user(user)
                .content(dto.getContent())
                .type(dto.getType())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Alarm saved = alarmRepository.save(alarm);
        rxSubjectManager.emit(user.getUserId(), saved);
        return saved;
    }

    // 해당 유저가 가지고 있는 알람 모두 가져오기
    public List<Alarm> findAll(CustomUserDetails userDetails) {

        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(()->new UserNotFoundException());

        List<Alarm> alarms = alarmRepository.findAlarmsByUserId(user.getUserId());

        if (alarms.isEmpty()) {
            throw new AlarmNotFoundException();
        }

        return alarms;
    }

    // 해당유저에게 알람보내주기
    public Observable<Alarm> subscribe(CustomUserDetails userDetails) {
        return rxSubjectManager.getObservableForUser(userDetails.getUserId());
    }

    // 로그아웃 시 subjectMap에서 제거
    public void unsubscribe(CustomUserDetails userDetails) {
        rxSubjectManager.unsubscribe(userDetails.getUserId());
    }

    // 유저의 알람 모두 읽음 표시
    @Transactional
    public int readAllAlarm(CustomUserDetails userDetails) {

        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        List<Alarm> alarms = alarmRepository.findByUserUserIdAndIsReadFalse(userDetails.getUserId());
        if (alarms.isEmpty()) {
            return 0;
        }

        // updateAlarmIsRead의 리턴 타입이 없을 때는
        // alarms.stream().forEach(alarm -> alarm.updateAlarmIsRead()); 못 쓴다.
        alarms.forEach(alarm -> {
            alarm.updateAlarmIsRead();
            alarmRepository.save(alarm);
        });

        return alarms.size();
    }

    // 유저의 특정 알람 읽음 표시
    @Transactional
    public AlarmResponseDTO readOneAlarm(CustomUserDetails userDetails, Long alarmId) {

        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        Alarm alarm = alarmRepository.findByAlarmIdAndIsReadFalse(alarmId)
                .orElseThrow(() -> new AlarmNotFoundException());

        alarm.updateAlarmIsRead();
        alarmRepository.save(alarm);

        return AlarmResponseDTO.builder()
                .alarmId(alarm.getAlarmId())
                .content(alarm.getContent())
                .isRead(alarm.getIsRead())
                .build();
    }


}