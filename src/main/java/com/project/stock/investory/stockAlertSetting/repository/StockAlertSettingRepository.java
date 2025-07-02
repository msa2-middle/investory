package com.project.stock.investory.stockAlertSetting.repository;

import com.project.stock.investory.stockAlertSetting.model.StockAlertSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface StockAlertSettingRepository extends JpaRepository<StockAlertSetting, Long> {

    List<StockAlertSetting> findByUserUserId(Long userId);

    Optional<StockAlertSetting> findByUserUserIdAndStockStockId(Long userId, String stockId);

    List<StockAlertSetting> findByIsActiveTrue();

    // 🔥 활성化된 알람 설정의 종목 코드들을 조회
    @Query("SELECT DISTINCT s.stock.stockId FROM StockAlertSetting s WHERE s.isActive = 1")
    List<String> findActiveStockCodes();

    // 🔥 특정 종목의 활성화된 알람 수 조회
    @Query("SELECT COUNT(s) FROM StockAlertSetting s WHERE s.stock.stockId = :stockCode AND s.isActive = 1")
    long countActiveAlertsByStockCode(@Param("stockCode") String stockCode);
}

