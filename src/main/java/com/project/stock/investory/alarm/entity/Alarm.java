package com.project.stock.investory.alarm.entity;

import com.project.stock.investory.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Alarm {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alarm_seq")
    @SequenceGenerator(name = "alarm_seq", sequenceName = "alarm_seq", allocationSize = 1)
    @Column(name = "alarm_id")
    private Long alarmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlarmType type;

    @Lob
    private String content;

    @Column(nullable = false)
    private Integer isRead;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    // 핵심: 단일 FK로 모든 관련 엔티티 참조 (NULL 값 없음!)
    @Column(name = "related_entity_id", nullable = false, length = 50)
    private String relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_entity_type", nullable = false, length = 20)
    private RelatedEntityType relatedEntityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (isRead == null) {
            isRead = 0;
        }
    }

    public void updateAlarmIsRead() {
        this.isRead = 1;
    }
}


