package com.project.stock.investory.stockAlertSetting.repository;

import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface StockAlertSettingRepository extends JpaRepository<StockAlertSetting, Long> {

    List<StockAlertSetting> findByUserUserId(Long userId);

    Optional<StockAlertSetting> findByUserUserIdAndStockStockId(Long userId, String stockId);

    List<StockAlertSetting> findByIsActiveTrue();

    @Transactional // 데이터 변경은 트랜잭션 안에서 실행되어야 함. 그래서 붙여줌
    @Modifying // JPA가 이 쿼리가 데이터 변경을 한다는 걸 인식시킴 (UPDATE, DELETE 등)
    @Query("UPDATE StockAlertSetting s SET s.isActive = false WHERE s.settingId = :settingId")
    int updateIsActiveFalseById(Long settingId);
}


