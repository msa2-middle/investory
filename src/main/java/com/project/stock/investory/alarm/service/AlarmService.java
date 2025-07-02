package com.project.stock.investory.alarm.service;

import com.project.stock.investory.alarm.dto.*;
import com.project.stock.investory.alarm.entity.Alarm;
import com.project.stock.investory.alarm.exception.AlarmNotFoundException;
import com.project.stock.investory.alarm.exception.UserNotFoundException;
import com.project.stock.investory.alarm.repository.AlarmRepository;
import com.project.stock.investory.alarm.exception.AuthenticationRequiredException;
import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.stockInfo.repository.StockRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final StockRepository stockRepository;
    private final RxSubjectManager rxSubjectManager;

    @Transactional
    public Alarm createAlarm(AlarmRequestDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        Alarm alarm = Alarm.builder()
                .user(user)
                .type(dto.getType())
                .content(dto.getContent())
                .targetUrl(dto.getTargetUrl())
                .relatedEntityId(dto.getRelatedEntityId())
                .relatedEntityType(dto.getRelatedEntityType())
                .sender(dto.getSender())
                .build();

        Alarm saved = alarmRepository.save(alarm);

        AlarmResponseDTO responseDTO = AlarmResponseDTO.from(saved);
        rxSubjectManager.emit(user.getUserId(), responseDTO);

        return saved;
    }

    // 기본 조회 (연관관계 정보 없음 - 빠름)
    public List<AlarmResponseDTO> findAll(CustomUserDetails userDetails) {
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        List<Alarm> alarms = alarmRepository.findAlarmsByUserId(userDetails.getUserId());

        if (alarms.isEmpty()) {
            throw new AlarmNotFoundException();
        }

        return alarms.stream()
                .map(AlarmResponseDTO::from)
                .collect(Collectors.toList());
    }

    // 상세 조회 (연관관계 정보 포함 - 필요할 때만)
    public List<AlarmResponseDTO> findAllWithDetails(CustomUserDetails userDetails) {
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        List<Alarm> alarms = alarmRepository.findAlarmsByUserId(userDetails.getUserId());

        if (alarms.isEmpty()) {
            throw new AlarmNotFoundException();
        }

        return alarms.stream()
                .map(alarm -> {
                    Object relatedInfo = getRelatedEntityInfo(alarm);
                    return AlarmResponseDTO.fromWithRelatedInfo(alarm, relatedInfo);
                })
                .collect(Collectors.toList());
    }

    // 연관 엔티티 정보 조회 (Repository 패턴 활용)
    private Object getRelatedEntityInfo(Alarm alarm) {
        switch (alarm.getRelatedEntityType()) {
            case POST:
                Long postId = Long.valueOf(alarm.getRelatedEntityId());  // String → Long 변환
                return postRepository.findById(postId)
                        .map(PostSummaryDTO::from)
                        .orElse(null);
            case COMMENT:
                Long commentId = Long.valueOf(alarm.getRelatedEntityId());  // String → Long 변환
                return commentRepository.findByCommentId(commentId)
                        .map(CommentSummaryDTO::from)
                        .orElse(null);
            case STOCK:
                String stockId = alarm.getRelatedEntityId();  // String 그대로 사용
                return stockRepository.findById(stockId)
                        .map(StockSummaryDTO::from)
                        .orElse(null);
            default:
                return null;
        }
    }

    // 특정 알람의 연관 엔티티 조회
    public <T> T getRelatedEntity(Long alarmId, Class<T> entityClass) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new AlarmNotFoundException());

        switch (alarm.getRelatedEntityType()) {
            case POST:
                if (entityClass == Post.class) {
                    Long postId = Long.valueOf(alarm.getRelatedEntityId());  // String → Long 변환
                    return (T) postRepository.findById(postId).orElse(null);
                }
                break;
            case COMMENT:
                if (entityClass == Comment.class) {
                    Long commentId = Long.valueOf(alarm.getRelatedEntityId());  // String → Long 변환
                    return (T) commentRepository.findByCommentId(commentId).orElse(null);
                }
                break;
            case STOCK:
                if (entityClass == Stock.class) {
                    String stockId = alarm.getRelatedEntityId();  // String 그대로 사용
                    return (T) stockRepository.findById(stockId).orElse(null);
                }
                break;
        }
        return null;
    }

    public Observable<AlarmResponseDTO> subscribe(CustomUserDetails userDetails) {
        return rxSubjectManager.getObservableForUser(userDetails.getUserId());
    }

    public void unsubscribe(CustomUserDetails userDetails) {
        rxSubjectManager.unsubscribe(userDetails.getUserId());
    }

    @Transactional
    public int readAllAlarm(CustomUserDetails userDetails) {
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        List<Alarm> alarms = alarmRepository.findByUser_UserIdAndIsRead(userDetails.getUserId(), 0);
        if (alarms.isEmpty()) {
            return 0;
        }

        alarms.forEach(alarm -> {
            alarm.updateAlarmIsRead();
            alarmRepository.save(alarm);
        });

        return alarms.size();
    }

    @Transactional
    public AlarmResponseDTO readOneAlarm(CustomUserDetails userDetails, Long alarmId) {
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        Alarm alarm = alarmRepository.findByAlarmIdAndIsRead(alarmId, 0)
                .orElseThrow(() -> new AlarmNotFoundException());

        alarm.updateAlarmIsRead();
        alarmRepository.save(alarm);

        return AlarmResponseDTO.from(alarm);
    }
}