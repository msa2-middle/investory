package com.project.stock.investory.stockAlertSetting.model;


import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_alert_setting",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "stock_id"})
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class StockAlertSetting {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle 12 버전부터 적용되는 코드
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_alert_setting_seq")
    @SequenceGenerator(name = "stock_alert_setting_seq", sequenceName = "stock_alert_setting_seq", allocationSize = 1)
    @Column(name = "stock_alert_setting_seq_id")
    private Long settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private int targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType condition;

    @Column(nullable = false)
    private Integer isActive;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public void updateSetting(int targetPrice, ConditionType condition) {
        if (targetPrice <= 0) {
            throw new IllegalArgumentException("유효하지 않은 목표가입니다.");
        }
        this.targetPrice = targetPrice;
        this.condition = condition;
    }

    @PrePersist
    public void prePersist() {
        if (isActive == null) {
            isActive = 1;  // 기본값 1으로 셋팅 - true
        }
    }

    public void updateIsActive() {
        this.isActive = 0;
    }

}
